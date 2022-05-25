package app.revanced.utils.patcher

import app.revanced.cli.MainCommand
import app.revanced.patcher.Patcher
import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.base.Data
import app.revanced.patcher.extensions.findAnnotationRecursively
import app.revanced.patcher.patch.base.Patch
import app.revanced.patcher.util.patch.implementation.JarPatchBundle

fun Patcher.addPatchesFiltered(
    packageCompatibilityFilter: Boolean = true,
    packageVersionCompatibilityFilter: Boolean = true,
    includeFilter: Boolean = false
) {
    val packageName = this.packageName
    val packageVersion = this.packageVersion

    MainCommand.patchBundles.forEach { bundle ->
        val includedPatches = mutableListOf<Patch<Data>>()
        JarPatchBundle(bundle).loadPatches().forEach patch@{ p ->
            val patch = p.getDeclaredConstructor().newInstance()

            val compatibilityAnnotation = patch.javaClass.findAnnotationRecursively(Compatibility::class.java)

            val patchName = patch.javaClass.findAnnotationRecursively(Name::class.java)?.name ?: Name::class.java.name

            val prefix = "[skipped] $patchName"

            if (includeFilter && !MainCommand.includedPatches.contains(patchName)) {
                println(prefix)
                return@patch
            }

            if (packageVersionCompatibilityFilter || packageCompatibilityFilter) {

                if (compatibilityAnnotation == null) {
                    println("$prefix: Missing compatibility annotation.")
                    return@patch
                }


                compatibilityAnnotation.compatiblePackages.forEach { compatiblePackage ->
                    if (packageCompatibilityFilter && compatiblePackage.name != packageName) {
                        println("$prefix: Package name not matching ${compatiblePackage.name}.")
                        return@patch
                    }

                    if (!packageVersionCompatibilityFilter || compatiblePackage.versions.any { it == packageVersion }) return@patch
                    println("$prefix: Unsupported version.")
                    return@patch
                }
            }

            includedPatches.add(patch)
            println("[added] $patchName")
        }
        this.addPatches(includedPatches)
    }
}

fun Patcher.applyPatchesPrint() {
    this.applyPatches().forEach { (patch, result) ->
        if (result.isSuccess) {
            println("[success] $patch")
            return@forEach
        }
        println("[error] $patch:")
        result.exceptionOrNull()!!.printStackTrace()
    }
}

fun Patcher.mergeFiles() {
    this.addFiles(MainCommand.mergeFiles)
}