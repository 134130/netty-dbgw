package com.github.l34130.netty.dbgw.core.policy

import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.config.GroupVersionKind
import com.github.l34130.netty.dbgw.policy.api.config.ResourceFactory
import com.github.l34130.netty.dbgw.policy.api.config.ResourceManifest
import com.github.l34130.netty.dbgw.policy.api.config.ResourceManifestMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds
import java.util.ServiceLoader
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class FilePolicyConfigurationLoader(
    private val file: File,
) : PolicyConfigurationLoader {
    private val watchService = FileSystems.getDefault().newWatchService()
    private val listeners = CopyOnWriteArrayList<PolicyChangeListener>()
    private val logger = KotlinLogging.logger("${this::class.java.name}-${file.path}")
    private val lock = ReentrantReadWriteLock()
    private val lastFiles = mutableListOf<Pair<File, PolicyDefinition>>()

    init {
        if (file.isFile) {
            file.parentFile.toPath()
        } else {
            file.toPath()
        }.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY,
        )

        executors.submit {
            while (true) {
                val key = watchService.take()
                key.pollEvents().forEach { event ->
                    val kind = event.kind()
                    val context = event.context() as File
                    val absoluteFile = file.resolve(context)
                    if (file.isFile && absoluteFile != file) {
                        // Skip events for the main file if it's a directory
                        return@forEach
                    }

                    when (kind) {
                        StandardWatchEventKinds.ENTRY_CREATE -> {
                            logger.info { "File created: $absoluteFile" }

                            // Notify listeners about the new policy
                            lock.write {
                                val newPolicy = readFromFile(absoluteFile)
                                lastFiles.addAll(newPolicy)
                                listeners.forEach { it.onPolicyAdded(newPolicy.first().second) }
                            }
                        }
                        StandardWatchEventKinds.ENTRY_DELETE -> {
                            logger.info { "File deleted: $absoluteFile" }

                            // Notify listeners about the removed policy
                            lock.write {
                                val index = lastFiles.indexOfFirst { it.first.absolutePath == absoluteFile.absolutePath }
                                if (index != -1) {
                                    val removed = lastFiles.removeAt(index).second
                                    listeners.forEach { it.onPolicyRemoved(removed) }
                                }
                            }
                        }
                        StandardWatchEventKinds.ENTRY_MODIFY -> {
                            logger.info { "File modified: $absoluteFile" }

                            // Notify listeners about the modified policy
                            lock.write {
                                val modifiedPolicy = readFromFile(absoluteFile)
                                val index = lastFiles.indexOfFirst { it.first.absolutePath == absoluteFile.absolutePath }
                                if (index != -1) {
                                    lastFiles[index] = modifiedPolicy.first()
                                    listeners.forEach { it.onPolicyAdded(modifiedPolicy.first().second) }
                                } else {
                                    lastFiles.addAll(modifiedPolicy)
                                    listeners.forEach { it.onPolicyAdded(modifiedPolicy.first().second) }
                                }
                            }
                        }
                    }
                }
                key.reset()
            }
        }
    }

    override fun load(): List<PolicyDefinition> {
        require(file.exists()) { "Policy file or directory does not exist: ${file.absolutePath}" }

        return lock.read {
            val policyDefinitions: List<Pair<File, PolicyDefinition>> =
                if (file.isFile) {
                    readFromFile(file)
                } else {
                    val policies = mutableListOf<Pair<File, PolicyDefinition>>()
                    file.walk().forEach { file ->
                        if (file.isFile && file.extension == "yaml" || file.extension == "yml") {
                            policies.addAll(readFromFile(file))
                        }
                    }
                    policies
                }

            lastFiles.addAll(policyDefinitions)
            policyDefinitions.map { it.second }
        }
    }

    override fun watchForChanges(listener: PolicyChangeListener): AutoCloseable {
        listeners.add(listener)

        return AutoCloseable {
            lock.write {
                listeners.remove(listener)
            }
        }
    }

    private fun readFromFile(file: File): List<Pair<File, PolicyDefinition>> {
        require(!file.isDirectory) { "Expected a file, but got a directory: ${file.absolutePath}" }
        require(file.exists()) { "Policy file does not exist: ${file.absolutePath}" }
        require(file.extension == "yaml" || file.extension == "yml") { "Expected a YAML file, but got: ${file.extension}" }

        val manifests: List<Pair<GroupVersionKind, ResourceManifest>> =
            ResourceManifestMapper
                .readValues(file)
                .map { it.groupVersionKind() to it }

        val policyDefinitions = mutableListOf<Pair<File, PolicyDefinition>>()
        ServiceLoader.load(ResourceFactory::class.java).forEach { factory ->
            for ((gvk, manifest) in manifests) {
                if (factory.isApplicable(gvk)) {
                    logger.debug { "Creating policy definition for $gvk from file ${file.absolutePath}" }
                    policyDefinitions.add(file to factory.create(manifest.spec) as PolicyDefinition)
                }
            }
        }
        return policyDefinitions
    }

    companion object {
        private val executors = Executors.newCachedThreadPool()
    }
}
