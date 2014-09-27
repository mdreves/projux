/** Copyright 2012-2013 Mike Dreves
  *
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at:
  *
  *     http://opensource.org/licenses/eclipse-1.0.php
  *
  * By using this software in any fashion, you are agreeing to be bound
  * by the terms of this license. You must not remove this notice, or any
  * other, from this software. Unless required by applicable law or agreed
  * to in writing, software distributed under the License is distributed
  * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
  * either express or implied.
  *
  * @author Mike Dreves
  */
package sbtprojux

import scala.io.Source

import sbt._
import sbt.Keys._
import Commands._
import Keys._

object SbtProjux extends Plugin {

  def projuxSettings: Seq[Setting[_]] = {
    commandSettings ++
    commonSettings ++
    inConfig(Compile)(compileSettings) ++
    inConfig(Test)(testSettings)
  }

  def commandSettings: Seq[Setting[_]] = {
    var cmds = Seq(
        Commands.share, Commands.deploy, Commands.gendocs, Commands.search,
        Commands.sanity, Commands.geterrors, Commands.geturl, Commands.openbug,
        Commands.openreview, Commands.openbuild, Commands.opentest,
        Commands.opencoverage)
    if (includeNonScala) {
      cmds ++= Seq(Commands.run, Commands.pkg)
    }
    Seq(commands ++= cmds)
  }

  def commonSettings: Seq[Setting[_]] = {
    var settings = Seq[Setting[_]](
      name := projectName,

      baseDirectory := file(projectDir),

      // Format
      format <<= (projuxFormat in Compile, projuxFormat in Test) map {
        (_, _) => Seq[java.io.File]()
      },

      // Lint
      lint <<= (projuxLint in Compile, projuxLint in Test) map {
        (_, _) => Seq[java.io.File]()
      }
    )

    // Compile (Scala compile)
    if (includeNonScala) {
      // If non-scala dependencies used, make scala compile depend on them
      settings ++= Seq(
          sbt.Keys.compile in Compile <<= projuxBuild in Compile map {
            // Convert Seq[File] to sbt.inc.Analysis to create dependency
            _ => sbt.inc.Analysis.Empty

          },
          sbt.Keys.compile in Test <<= projuxBuild in Test map {
            // Convert Seq[File] to sbt.inc.Analysis to create dependency
            _ => sbt.inc.Analysis.Empty
          }
      )
    }

    // Build
    if (includeScala) {
      // If scala part of project, then build dependends on scala compile
      // (which depends on projuxBuild if non-scala files)
      settings ++= Seq(
          build <<= (sbt.Keys.compile in Compile,sbt.Keys.compile in Test) map {
            // Convert sbt.inc.Analysis into Seq[File] to create dependency
            (_, _) => Seq[java.io.File]()
          }
      )
    } else {
      // If scala not part of project then depend directly on projuxBuild
      settings ++= Seq(
          build <<= (projuxBuild in Compile, sbt.Keys.compile in Test) map {
            (_, _) => Seq[java.io.File]()
          }
      )
    }

    // Test
    if (includeNonScala) {
      settings ++= Seq(sbt.Keys.test <<= projuxTest in Test map { _ => () })
    }

    // Status
    settings ++= Seq(
        projuxStatus <<= (
          name,
          streams in (Compile, projuxFormat),
          streams in (Test, projuxFormat),
          streams in (Compile, projuxLint),
          streams in (Test, projuxLint),
          streams in (Compile, projuxBuild),
          streams in (Test, projuxBuild),
          streams in (Compile, projuxTest),
          streams in (Test, projuxTest),
          streams in (Compile, projuxCoverage),
          streams in (Test, projuxCoverage)
        ) map Tasks.status,
        status <<= projuxStatus
    )

    // Clean
    settings ++= Seq(
        target in projuxClean := file(projectTargetDir),
        projuxClean <<= (
          target in projuxClean,
          name,
          streams
        ) map Tasks.clean,
        // This makes 'clean' command dependent on 'projux-clean' as well
        sbt.Keys.clean <<= (
          sbt.Keys.cleanFiles,
          sbt.Keys.cleanKeepFiles,
          projuxClean
        ) map { (clean, keep, _) => sbt.Defaults.doClean(clean, keep) }
    )

    settings
  }

  def compileSettings = Seq(
    unmanagedSources := sourceFiles,

    // Format
    projuxFormat <<= (
      unmanagedSources,
      name,
      configuration,
      streams
    ) map Tasks.format,

    // Lint
    projuxLint <<= (
      unmanagedSources,
      name,
      configuration,
      streams
    ) map Tasks.lint,

    // Build
    projuxBuild <<= (
      unmanagedSources,
      name,
      configuration,
      streams
    ) map Tasks.build
  )

  def testSettings = Seq(
    unmanagedSources := testFiles,

    // Format
    projuxFormat <<= (
      unmanagedSources,
      name,
      configuration,
      streams
    ) map Tasks.format,

    // Lint
    projuxLint <<= (
      unmanagedSources,
      name,
      configuration,
      streams
    ) map Tasks.lint,

    // Build
    projuxBuild <<= (
      unmanagedSources,
      name,
      configuration,
      streams
    ) map Tasks.build,

    // Test
    projuxTest <<= (
      unmanagedSources,
      name,
      configuration,
      streams
    ) map Tasks.test
  )

  lazy val projectName: String = env("PROJECT_NAME")
  lazy val projectDir: String = env("PROJECT_DIR")
  lazy val projectSrcDir: String = env("PROJECT_SRC_DIR")
  lazy val projectTestDir: String = env("PROJECT_TEST_DIR")
  lazy val projectTargetDir: String = env("PROJECT_TARGET_DIR")
  lazy val projectBinDir: String = env("PROJECT_BIN_DIR")
  lazy val projectGenDir: String = env("PROJECT_GEN_DIR")
  lazy val projectPkgs: Seq[String] = env("PROJECT_PKGS").split("\\s+")
  lazy val projectIncludeFilter: Seq[String] = {
    var filter = env("PROJECT_INCLUDE_FILTER")
    if (filter.isEmpty) filter = env("DEFAULT_PROJECT_INCLUDE_FILTER")
    for (f <- filter.split(":")) yield f
  }
  lazy val projectExcludeFilter: Seq[String] = {
    var filter = env("PROJECT_EXCLUDE_FILTER")
    if (filter.isEmpty) filter = env("DEFAULT_PROJECT_EXCLUDE_FILTER")
    for (f <- filter.split(":")) yield f
  }
  lazy val projectTestSuffixes: Seq[String] =
    env("PROJECT_TEST_SUFFIXES").split("\\s+")
  lazy val includeScala: Boolean =
    (projectIncludeFilter filter(_ == "*.scala")).size > 0
  lazy val includeNonScala: Boolean =
    (projectIncludeFilter filter(_ != "*.scala")).size > 0

  lazy val sourceFiles: Seq[java.io.File] = {
    val xs = selectFiles(selectDirs(Seq(projectSrcDir)))
    if (projectSrcDir == projectTestDir) {
      xs.filter(! testFiles.contains(_))
    } else {
      xs
    }
  }

  lazy val testFiles: Seq[java.io.File] = {
    val xt = selectFiles(selectDirs(Seq(projectTestDir)))
    if (! projectTestSuffixes.isEmpty) {
      // Regex .*(_test)\.*[^.]*" will match foo_test.xxx, etc
      val p = java.util.regex.Pattern.compile(
          ".*(" + projectTestSuffixes.mkString("|") + ")\\.[^.]*")
      xt.filter((f: java.io.File) => p.matcher(f.getName).matches)
    } else {
      xt
    }
  }

  def env(name: String): String =
    sys.env.getOrElse(name, "")

  def selectDirs(bases: Seq[String]): Seq[java.io.File] = {
    for {
      pkg <- projectPkgs
      base <- bases
    } yield file(base + "/" + pkg)
  }

  def selectFiles(dirs: Seq[java.io.File],
                  suffixes: Seq[String] = Seq()): Seq[java.io.File] = {
    val includeFiles =
        dirs ** (projectIncludeFilter filter(_ != "*.scala") mkString(" || "))
    val excludeFiles =
        dirs ** (projectExcludeFilter filter(_ != "*.scala") mkString(" || "))
    (includeFiles --- excludeFiles).get
  }
}
