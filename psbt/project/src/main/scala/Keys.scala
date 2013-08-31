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

import sbt._

object Keys {
  // Common keys (without scala alternatives)
  val format: TaskKey[Seq[File]] = TaskKey[Seq[File]]("format", "Format")

  val lint: TaskKey[Seq[File]] = TaskKey[Seq[File]]("lint", "Lint")

  val build: TaskKey[Seq[File]] = TaskKey[Seq[File]]("build", "Build")

  val status: TaskKey[Unit] = TaskKey[Unit]("status", "Status")

  // Projux keys
  val projuxFormat: TaskKey[Seq[File]] = TaskKey[Seq[File]](
      "projux-format", "Projux format")

  val projuxLint: TaskKey[Seq[File]] = TaskKey[Seq[File]](
      "projux-lint", "Projux lint")

  val projuxBuild: TaskKey[Seq[File]] = TaskKey[Seq[File]](
      "projux-build", "Projux build")

  val projuxTest: TaskKey[Seq[File]] = TaskKey[Seq[File]](
      "projux-test", "Projux test")

  val projuxCoverage: TaskKey[Seq[File]] = TaskKey[Seq[File]](
      "projux-coverage", "Projux coverage")

  val projuxStatus: TaskKey[Unit] = TaskKey[Unit](
      "projux-status", "Projux status")

  val projuxClean: TaskKey[Unit] = TaskKey[Unit](
      "projux-clean", "Projux clean")
}
