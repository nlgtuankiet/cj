@file:Suppress("DEPRECATION")

package com.rainyseason.wmmp

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.BaseExtension
import javassist.ClassPool
import javassist.CtClass
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.logging.Logger
import java.io.FileOutputStream
import java.util.Collections
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class WorkManagerMinPeriodicTransform(
    private val baseExtension: BaseExtension,
    private val logger: Logger,
) : Transform() {

    override fun getName(): String {
        return "WorkManagerMinPeriodic"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return Collections.singleton(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return Collections.singleton(QualifiedContent.Scope.EXTERNAL_LIBRARIES)
    }

    override fun isIncremental(): Boolean {
        return true
    }

    override fun transform(transformInvocation: TransformInvocation) {
        val isIncremental = transformInvocation.isIncremental
        if (!isIncremental) {
            transformInvocation.outputProvider.deleteAll()
        }

        transformInvocation.inputs.forEach { transformInput ->
            check(transformInput.directoryInputs.isEmpty())
            transformInput.jarInputs.forEach { jarInput ->
                val jarName = jarInput.name
                val file = jarInput.file
                val dest = transformInvocation.outputProvider.getContentLocation(
                    jarName,
                    jarInput.contentTypes, jarInput.scopes, Format.JAR
                )
                val status = jarInput.status

                if (status == Status.REMOVED) {
                    FileUtils.deleteQuietly(dest)
                } else if (!isIncremental || status != Status.NOTCHANGED) {
                    if (
                        file.name.startsWith("work-runtime")
                        && !file.name.startsWith("work-runtime-ktx")
                    ) {
                        logger.warn("Patching $file")
                        val sdkDir = baseExtension.sdkDirectory.absolutePath
                        val androidJar = "${sdkDir}/platforms/${baseExtension.compileSdkVersion}/android.jar"
                        val classPool = ClassPool()
                        classPool.appendSystemPath()
                        classPool.insertClassPath(file.toString())
                        classPool.insertClassPath(androidJar)

                        val modifiedClasses = mutableListOf<CtClass>()
                        val longClazz = classPool.get("long")
                        modifiedClasses += classPool.getCtClass("androidx.work.impl.model.WorkSpec")
                            .apply {
                                with(getDeclaredMethod("setPeriodic", arrayOf(longClazz))) {
                                    setBody(
                                        """
                                    { this.setPeriodic($1, $1); }
                                """.trimIndent()
                                    )
                                }
                                with(
                                    getDeclaredMethod(
                                        "setPeriodic",
                                        arrayOf(longClazz, longClazz)
                                    )
                                ) {
                                    setBody(
                                        """
                                    {
                                        this.intervalDuration = $1;
                                        this.flexDuration = $2;
                                    }
                                """.trimIndent()
                                    )
                                }
                            }

                        val input = JarFile(file)
                        val jarOutputStream = JarOutputStream(FileOutputStream(dest))

                        input.entries().iterator().forEach { jarEntry ->
                            val replaced = modifiedClasses.any {
                                jarEntry.name == "${it.name.replace(".", "/")}.class"
                            }
                            if (!replaced) {
                                val s = input.getInputStream(jarEntry)
                                jarOutputStream.putNextEntry(JarEntry(jarEntry.name))
                                IOUtils.copy(s, jarOutputStream)
                                s.close()
                            }
                        }
                        modifiedClasses.forEach {
                            val name = "${it.name.replace(".", "/")}.class"
                            logger.warn("Writing $name")
                            val byteCode = it.toBytecode()
                            jarOutputStream.putNextEntry(JarEntry(name))
                            jarOutputStream.write(byteCode)
                        }

                        jarOutputStream.close()
                    } else {
                        FileUtils.copyFile(file, dest)
                    }
                }
            }
        }
    }
}