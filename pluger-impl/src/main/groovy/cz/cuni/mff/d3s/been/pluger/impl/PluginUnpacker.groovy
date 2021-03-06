package cz.cuni.mff.d3s.been.pluger.impl

import cz.cuni.mff.d3s.been.pluger.IPluginUnpacker

import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

class PluginUnpacker implements IPluginUnpacker {

    @Override
    void unpack(PlugerConfig config, Collection<PluginDescriptor> pluginDescriptors) {
        unpackDependenciesAndPlugins(config.unpackedLibsDirectory, pluginDescriptors)
    }

    private Collection<Path> unpackDependenciesAndPlugins(Path pluginsDir, Collection<PluginDescriptor> pluginDescriptors) {
        pluginDescriptors.collect {
            unpackDependenciesAndPlugin(pluginsDir, it)
        }.flatten()
    }

    private Collection<Path> unpackDependenciesAndPlugin(Path pluginsDir, PluginDescriptor pluginDescriptor) {
        def zipPluginFile = new ZipFile(pluginDescriptor.pluginPath.toFile())
        pluginDescriptor.dependencies.each {
            unpackDependencyOrPlugin(Type.DEPENDENCY, it.groupId, it.artifactId, it.version, zipPluginFile, pluginsDir)
        }

        unpackDependencyOrPlugin(Type.PLUGIN, pluginDescriptor.groupId, pluginDescriptor.artifactId, pluginDescriptor.version, zipPluginFile, pluginsDir)

        zipPluginFile.close()
    }

    private void unpackDependencyOrPlugin(Type type, String groupId, String artifactId, String version, ZipFile zipFile, Path pluginsDir) {
        def relativeJarParentPath = groupId.split("\\.") + artifactId + version
        def jarName = "${artifactId}-${version}.jar" as String
        def relativeJarPath = ['lib'] + relativeJarParentPath.flatten() + jarName
        def jarDependencyEntry = zipFile.getEntry(type == Type.DEPENDENCY ? relativeJarPath.join("/") : jarName)

        def onDiskJarParentPath = pluginsDir
        relativeJarParentPath.each {
            onDiskJarParentPath = onDiskJarParentPath.resolve(it)
        }

        def onDiskJarPath = onDiskJarParentPath.resolve(Paths.get(jarName))
        Files.createDirectories(onDiskJarParentPath)
        if (Files.notExists(onDiskJarPath) || version.endsWith("-SNAPSHOT")) {
            // TODO  missing unit test that snapshot dependencies are always unpacked
            Files.copy(zipFile.getInputStream(jarDependencyEntry), onDiskJarPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private enum Type {
        DEPENDENCY,
        PLUGIN
    }

}
