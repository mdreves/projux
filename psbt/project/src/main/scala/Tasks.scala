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

import scala.language.postfixOps  // For use with ! operator

import sbt._
import sbt.Keys._

object Tasks {
  def format(unmanagedSources: Seq[File], projectName: String,
             config: Configuration, streams: TaskStreams) = {
    processTask("format", unmanagedSources, projectName, config, streams)
  }

  def lint(unmanagedSources: Seq[File], projectName: String,
           config: Configuration, streams: TaskStreams) = {
    processTask("lint", unmanagedSources, projectName, config, streams)
  }

  def build(unmanagedSources: Seq[File], projectName: String,
            config: Configuration, streams: TaskStreams) = {
    processTask("build", unmanagedSources, projectName, config, streams)
  }

  def test(unmanagedSources: Seq[File], projectName: String,
           config: Configuration, streams: TaskStreams) = {
    processTask("test", unmanagedSources, projectName, config, streams)
  }

  def coverage(unmanagedSources: Seq[File], projectName: String,
               config: Configuration, streams: TaskStreams) = {
    processTask("coverage", unmanagedSources, projectName, config, streams)
  }

  def status(projectName: String, formatCompileStreams: TaskStreams,
             formatTestStreams: TaskStreams, lintCompileStreams: TaskStreams,
             lintTestStreams: TaskStreams, buildCompileStreams: TaskStreams,
             buildTestStreams: TaskStreams, testCompileStreams: TaskStreams,
             testTestStreams: TaskStreams, coverageCompileStreams: TaskStreams,
             coverageTestStreams: TaskStreams) {
    printErrorFiles("compile:format", formatCompileStreams)
    printErrorFiles("test:format", formatTestStreams)
    printErrorFiles("compile:lint", lintCompileStreams)
    printErrorFiles("test:lint", lintTestStreams)
    printErrorFiles("compile:build", buildCompileStreams)
    printErrorFiles("test:build", buildTestStreams)
    printErrorFiles("compile:test", testCompileStreams)
    printErrorFiles("test:test", testTestStreams)
    printErrorFiles("compile:coverage", coverageCompileStreams)
    printErrorFiles("test:coverage", coverageTestStreams)
  }

  def clean(targetDirectory: File, projectName: String, streams: TaskStreams) {
    val status = "clean" !  // newline or semicolon needed after !

    if (status != 0) streams.log.error("Error: %d".format(status))
  }

  private def processTask(task: String, unmanagedSources: Seq[File],
                          projectName: String, config: Configuration,
                          streams: TaskStreams) = {

    val cache = streams.cacheDirectory

    def logFn(format: String)(data: String) = {
      streams.log.info(format.format(data))
    }
    // Formatting, Linting, etc
    val progressiveTenseLogFn = logFn(
        progressiveTenseFormat(projectName, config.toString, task))_
    // Re-formatted, Re-linted, etc
    val pastTenseLogFn = logFn(
        pastTenseFormat(projectName, config.toString, task))_

    def taskFn(files: Set[File]): Set[java.io.File] = {
      val errorFile = errorCache(cache)
      val contents = if (errorFile.exists) IO.read(errorFile) else ""
      var buf = if (contents.size > 0) {
          collection.mutable.ListBuffer[String](
              (for (f <- contents.split("[\\r\\n]+")) yield f):_*)
        } else {
          collection.mutable.ListBuffer[String]()
        }
      var out = Set[java.io.File]()

      for {
        file <- files if file.exists
        name = file.toString()
      } {
        val status = task + " " + name !  // newline or semicolon needed after !

        if (status != 0) {
          if (!buf.exists(_ == name)) buf += name
          streams.log.error("Failure(%d): %s".format(status, name))
        } else {
          buf = buf.filter(_ != name)
          out += file
        }
      }

      IO.write(errorFile, buf.mkString("\n"))
      out
    }

    processFiles(unmanagedSources, cache, progressiveTenseLogFn, taskFn).toSeq
  }

  // Converts string to past/progressive tenses
  private def tense(str: String, prefix: String = "", suffix: String = "") = {
    val (start, last) = str splitAt (str.size - 1)
    if (("aeiou" contains (start takeRight 1)) && ! ("n" contains last))
      prefix + str + last + suffix
    else
      prefix + str + suffix
  }

  // Example: progressiveTenseFormat("project1", "compile', "format")
  //          "proj1:compile Formatting %s..."
  private def progressiveTenseFormat(
      projectName: String, config: String, task: String) = {
    projectName + ":" + config + " " +
    tense(task.capitalize, suffix="ing") + " %s..."
  }

  // Example: pastTenseFormat("project1", "compile', "format")
  //          "proj1:compile  Re-formatted %s."
  private def pastTenseFormat(
      projectName: String, config: String, task: String) = {
    projectName + ":" + config + " " +
    tense(task, prefix="Re-", suffix="ed") + " %s."
  }

  // Processes files that have changed since last time task run
  private def processFiles(files: Seq[File], cache: File,
                           logFn: String => Unit,
                           updateFn: Set[File] => Set[java.io.File]) = {

    def processUpdate(in: ChangeReport[File],
                      out: ChangeReport[File]): Set[java.io.File] = {
      val files = in.modified -- in.removed
      inc.Analysis.counted("File", "", "s", files.size) foreach logFn
      updateFn(files)
    }

    FileFunction.cached(cache)(
        FilesInfo.lastModified, FilesInfo.exists)(processUpdate)(files.toSet)
  }

  private def errorCache(cacheDir: File) = cacheDir / "error-cache"

  private def printErrorFiles(msg: String, streams: TaskStreams) {
    var cache = errorCache(streams.cacheDirectory)
    if (cache.exists) {
      val contents = IO.read(cache)
      if (contents.size > 0) {
        streams.log.info("-" * 73)
        streams.log.info("ERRORS: " + msg)
        streams.log.info("-" * 73)
        for (l <- contents.split("\n")) streams.log.info(l)
      }
    }
  }
}
