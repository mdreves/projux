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

object Commands {
  lazy val run = Command.args("run", "<flags>") { (state, args) =>
    val status = "run " + args.mkString(" ") !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }

  lazy val share = Command.args("share", "<program>") { (state, args) =>
    val status = "share " + args.mkString(" ") !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }

  lazy val pkg = Command.args("package", "<flags>") { (state, args) =>
    val status = "package " + args.mkString(" ") !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }

  lazy val deploy = Command.args("deploy", "<flags>") { (state, args) =>
    val status = "deploy " + args.mkString(" ") !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }

  lazy val gendocs = Command.args("gendocs", "<flags>") { (state, args) =>
    val status = "gendocs " + args.mkString(" ") !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }

  lazy val search = Command.args("search", "<patterns>") { (state, args) =>
    val status = "search " + args.mkString(" ") !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }

  lazy val sanity = Command.command("sanity") { state =>
    val status = "sanity" !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }

  lazy val geterrors = Command.args(
      "geterrors", "{:build|:lint|:test|:coverage}") { (state, args) =>
    val status = "geterrors " + args.mkString(" ") !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }

  lazy val geturl = Command.args(
      "geterrors", "{:build|:lint|:test|:coverage|:bug <id>|:review <id>}") {
        (state, args) =>
    val status = "geturl " + args.mkString(" ") !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }

  lazy val openbug = Command.args("openbug", "<id>") { (state, args) =>
    val status = "openbug " + args.mkString(" ") !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }

  lazy val openreview = Command.args("openreview", "<id>") { (state, args) =>
    val status = "openreview " + args.mkString(" ") !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }

  lazy val openbuild = Command.command("openbuild") { state =>
    val status = "openbuild" !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }

  lazy val opentest = Command.command("opentest") { state =>
    val status = "opentest" !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }

  lazy val opencoverage = Command.command("opencoverage") { state =>
    val status = "opencoverage" !  // newline or ; needed

    if (status != 0) state.globalLogging.full.error("Error: %d".format(status))
    state
  }
}
