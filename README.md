#Overview

Projux is a BASH script (.projectrc) that provides command line support for
managing one or more project working directories and related settings.
Built-in support is provided for managing the projects both locally and
remotely (via TMUX).

By default, commands are provided for connecting to remote workspaces,
switching between workspaces (automatically setting and clearing project
environment variables), and managing sessions. A framework is also provided
for implementing project specific functions for formatting, linting,
building, testing, etc based on a standard set of commands common to all
projects.

# Installation

Installation is straight forward:

1. Clone the repository to a local machine (e.g. Mac laptop, etc):

        mkdir .projux
        git clone git://github.com/mdreves/projux.git .projux

2. Source the projux `.projectrc` script from `~/.bashrc`

        if [ -f .projux/.projectrc ]; then
          . .projux/.projectrc
        fi

3. Add projects to `~/.projects`

4. If managing remote projects, setup remote hosts

    This can either be done using `project sync` or manually.  In either case,
    make sure to set the `PROJECT_HOST` vars. Alternatively, a
    `DEFAULT_PROJECT_HOST` can be set in `~/.bashrc` if all the projects run on
    the same server.

    If setting up manually, copy the `.projectrc` and `~/.projects` files to the
    remote host(s) and setup the remote `~/.bashrc file`.

    If using `project sync`, for each `PROJECT_HOST` that was set, set the
    corresponding `PROJECT_SYNC_LIST` to a list of what will be synchronized to
    the remote host. If `DEFAULT_PROJECT_HOST` is used, then set the
    `DEFAULT_PROJECT_SYNC_LIST`.  The sync list should at least include the
    `~/.projects` file. If you are like me and have a shared `.bashrc` file used
    between a Mac laptop and a linux desktop server, then you can add `.bashrc`
    and `.projectrc` to the sync list. If not, you will need to manually copy
    the `.projectrc` and setup the remote `.bashrc` to source it.  Lastely,
    synchronize the local and remote hosts:

    If separate PROJECT_HOST and PROJECT_SYNC_LIST settings:

        $ project sync <project_name>

    If using DEFAULT_PROJECT_HOST and DEFAULT_PROJECT_SYNC_LIST settings:

        $ project sync

# Quickstart

A basic workflow might be as follows:

* Setup project's enviroment variables and aliases:

        $ vi ~/.projects

          PROJECT_NAME=foo
          PROJECT_TYPE=scala
          PROJECT_HOST=overthere
          ...
          alias deploy='my_deploy_script'
          ...

          PROJECT_NAME=bar
          ...

    NOTE: All environment vars and aliases from the project matching
    `PROJECT_NAME` up to the next `PROJECT_NAME` entry will be loaded whenever
    the `project <project_name>` command is used.

* Setup project sync list and synchronize local/remote hosts

        $ vi ~/.projectsync

        .projects
        ...

        $ project sync overthere

* Attach to remote project

        $ project attach foo

    NOTE: By default this will attach to the first TMUX window listed in
    `DEFAULT_PROJECT_WINDOWS` with a TMUX session named after the project.

* Hack in vim, etc

* Open a second terminal session to project

        $ project attach foo 2

    NOTE: By default this will attach to the second TMUX window listed in
    `DEFAULT_PROJECT_WINDOWS` with a TMUX session named after the project
    but with `_2` append (e.g. `foo_2`).

* In second terminal window `build`, `test`, ...

        $ format
        $ lint
        $ build
        $ test
        ...

* Switch first terminal session to another project

        $ project bar

* Hack in vim in new project

* Switch to second terminal session of new project

        $ project bar 2
        $ build

* Temporarily update tests to a specific test

        $ settest bar_test
        $ test

* Reload original project settings

        $ project bar 2

* Switch first terminal back to first project...

        $ project foo

* Detach from terminal 1

        $ project detach

* Reattach right where we left off...

        $ project attach foo

...

# Commands

## project
    $ project

      Show current project.

    $ project ls

      Display list of known projects (from ~/.projects).

    $ project <project>

      If attached switch to <project>, if detached init <project> vars.

      Examples:
        project foo            :  Switch to project foo

    $ project attach [<project>] [<session>]

      Attach to TMUX session.

      Examples:
        project attach foo     :  Attach/switch to foo project
        project attach 2       :  Attach/switch to 2nd session for current project
        project attach foo 3   :  Attach/switch to 3rd session for project foo

    $ project detach [<project>] [<session>]

      Detach from TMUX session.

      Examples:
        project detach         :  Detach current client from cur project
        project detach foo     :  Detach all clients attached to foo project
        project detach foo 2   :  Detach all clients attached to 2nd session to foo

    $ project kill [<project>] [<session>]

      Kill TMUX session.

      Examples:
        project kill           :  Kill all of current project's TMUX sessions
        project kill foo       :  Kill all of foo project's TMUX sessions
        project kill foo 1     :  Kill main session of project foo
        project kill foo 2     :  Kill 2nd session of project foo

    $ project sessions

      Show all attached project sessions.

    $ project settings [<project>]

      Show project env var settings.

      Examples:
        project settings       :  Show current project settings
        project settings foo   :  Show project foo's env var settings

    $ project sync [<project>]

      Sync local files with remote project host based on $PROJECT_SYNC_LIST and
      $PROJECT_SYNC_DESTS.

      Examples:
        project sync           :  Sync current project
        project sync foo       :  Sync local files with foo's project host

    $ project backup [<project>]

      Backs up changed project files (not in .git repo) to $PROJECT_BACKUP_DIR.

      Examples:
        project backup         :  Backup uncomitted .git files
        project backup foo     :  Backup uncomited project foo .git files

## win
    $ win

      Display current TMUX window.

    $ win ls

      List current sessions TMUX windows.

    $ win [<win>]

      Switch to named window.

      Examples:
        win bash               :  Switch to window named bash

    $ win new [<win>]

      Create new TMUX window

    $ win session

      Switch to window associated with TMUX session group (foo_2 => 2).

## search
    $ search [-p <project>] <patterns>

      Runs project specific search based on $PROJECT_SEARCH_FN implementation.

      Examples:
        search xxx             :  Search for xxx in project code base
        search -p foo xxx      :  Search for xxx in project foo code base

## format
    $ format [-p <project>] <targets>

      Runs project specific format based on $PROJECT_FORMAT_FN implementation.

      Examples:
        format                 :  Format DEFAULT_PROJECT_FORMAT_TARGETS targets
        format ...             :  Format recursively from current dir
        format -p foo ...      :  Format recursively in project foo

## lint
    $ lint [-p <project>] <targets>

      Runs project specific lint based on $PROJECT_LINT_FN implementation.

      Examples:
        lint                   :  Lint DEFAULT_PROJECT_LINT_TARGETS targets
        lint ...               :  Lint recursively from current dir
        lint -p foo ...        :  Lint recursively in project foo

## build
    $ build [-p <project>] <targets>

      Runs project specific build based on $PROJECT_BUILD_FN implementation.

      Examples:
        build                  :  Build DEFAULT_PROJECT_BUILD_TARGETS targets
        build :all             :  Build :all target
        build :clean           :  Build :clean target
        build -p foo :all      :  Build :all target in project foo

## test
    $ test [-p <project>] <targets>

      Runs project specific test based on $PROJECT_TEST_FN implementation.

      Examples:
        test                   :  Test DEFAULT_PROJECT_TEST_TARGETS targets
        test :all              :  Test :all target
        test foo_class         :  Test foo_class target
        test -p foo :all       :  Test :all target in project foo

    $ gettest [-p <project>]

      Returns project default test targets ($PROJECT_DEFAULT_TEST_TARGETS).

    $ settest [-p <project>] <targets>

      Sets project default test targets ($PROJECT_DEFAULT_TEST_TARGETS).

      Examples:
        settest foo_class      :  DEFAULT_PROJECT_TEST_TARGETS=foo_class

    $ updatetest [-p <project>] {+,-}<targets>

      Add/remove to/from project default test targets (
      $PROJECT_DEFAULT_TEST_TARGETS).

      Examples:
        updatetest +foo        :  Add foo to DEFAULT_PROJECT_TEST_TARGETS
        updatetest -bar        :  Remove bar from DEFAULT_PROJECT_TEST_TARGETS

    $ cleartest [-p <project>]

      Clears project default test targets ($PROJECT_DEFAULT_TEST_TARGETS).

## coverage
    $ coverage [-p <project>] <targets>

      Runs project specific coverage based on $PROJECT_COVERAGE_FN implementation.

      Examples:
        coverage               :  Coverage DEFAULT_PROJECT_TEST_TARGETS targets
        coverage :all          :  Coverage :all target
        coverage foo_class     :  Coverage foo_class target
        coverage -p foo :all   :  Coverage :all target in project foo

## run
    $ run [-p <project>] <label> <flags>

      Runs project specific program based on $PROJECT_DEFAULT_RUN_CMDS setting.
      Multiple executables can be tagged with 'label::' labels to distinguish
      them. The label is then provided to the run command to select the
      appropriate program. If no label is provided then the first entry is used.

      Examples:
        PROJECT_DEFAULT_RUN_CMDS="client::client --f=a server::server --port=6"
        run                    :  $PROJECT_RUN_DIR/client --f=a
        run client             :  As above
        run client --f=-a,+b   :  $PROJECT_RUN_DIR/client --f=b
        run client --f=c       :  $PROJECT_RUN_DIR/client --f=c
        run shared server      :  $PROJECT_SHARE_DIR/server --port=6

    $ getrun [-p <project>] [<label>] [<flags>]

      Returns project default run commands ($PROJECT_DEFAULT_RUN_CMDS).

      Examples:
        PROJECT_DEFAULT_RUN_CMDS="client::client --f=a server::server --port=6"
        getrun                 : client::client --f=a server::server --port=6
        getrun client          : client --f=a
        getrun client --f=+b   : client --f=a,b

    $ setrun [-p <project>] <cmds>

      Sets project default run commands ($PROJECT_DEFAULT_RUN_CMDS).

      Examples:
        setrun client::client --f=a server::server --port=6

    $ updaterun [-p <project>] <label> <flags{+,-}>

      Add/remove flags to/from a project default run command (
      $PROJECT_DEFAULT_RUN_CMDS).

      Examples:
        updaterun client::client --f=-a,+b

    $ clearrun [-p <project>]

      Clears project default run commands ($PROJECT_DEFAULT_RUN_CMDS).

## share
    $ share [-p <project>] <prog>

      Copies named program file from $PROJECT_BIN_DIR to $PROJECT_SHARE_DIR.

      Examples:
        share client           :  cp $PROJECT_BIN_DIR/client $PROJECT_SHARED_DIR

## sanity
    $ sanity [-p <project>]

      Runs project specific sanity tests based on $PROJECT_SANITY_FN
      implementation.  Sanity tests typically involve one or more of format,
      lint, build, test, etc and are usually run before submitting code to
      ensure the check in is sane.

      Examples:
        sanity                 :  Run sanity for current project
        sanity -p foo          :  Run sanity on project foo

## Misc
    sshproj
      ssh -Y -t $PROJECT_HOST

    sftpproj
      sftp $PROJECT_HOST

    cdproject
      cd $PROJECT_DIR

    pushproject
      pushd $PROJECT_DIR

    cdbuild
      cd $PROJECT_BUILD_DIR

    pushbuild
      pushd $PROJECT_BUILD_DIR

    cdsrc (also cdsrc2, ..., cdsrc5)
      cd $PROJECT_SRC_DIR/$(arr=(${PROJECT_PKGS}); echo ${arr[0]})

    pushsrc (also pushsrc2, .. pushsrc5)
      pushd $PROJECT_SRC_DIR/$(arr=(${PROJECT_PKGS}); echo ${arr[0]})

    cdtest (also cdtest2, ..., cdtest5)
      cd $PROJECT_TEST_DIR/$(arr=(${PROJECT_PKGS}); echo ${arr[0]})

    pushtest (also pushtest2, ..., pushtest5)
      pushd $PROJECT_TEST_DIR/$(arr=(${PROJECT_PKGS}); echo ${arr[0]})

    cdgen (also cdgen2, ..., cdgen5)
      cd $PROJECT_GEN_DIR/$(arr=(${PROJECT_PKGS}); echo ${arr[0]})

    pushgen (also pushgen2, ..., pushgen5)
      pushd $PROJECT_GEN_DIR/$(arr=(${PROJECT_PKGS}); echo ${arr[0]})

    cdbin
      cd $PROJECT_BIN_DIR

    pushbin
      pushd $PROJECT_BIN_DIR

    geterrors {:build|:lint|:test|:coverage|<target>}
      Returns VIM quickfix friendly errors for last build/lint/test/coverage.
      Errors should be echoed in the form: <filename>:<line>:<col>:<message>.
      If called with ':build', ':lint', ':test', or ':coverage' then errors
      from the last build/lint/test/coverage should be returned. Otherwise
      the errors for the passed in target should be returned.

      Examples:
        geterrors :build            # Errors from last build
        geterrors :lint             # Errors from last lint
        geterrors :test             # Errors from last test
        geterrors :coverage         # Errors from last coverage
        geterrors foo.py            # Current errors for file foo.py

    geturl {:build|:test|:coverage|:bug <id>|:review <id>}
      Returns URL with results for last build/test/coverage or for a bug/review.

    openbug <id>
      Opens browser to URL for bug <id>

    openreview <id>
      Opens browser to URL for review <id>

    openbuild
      Opens browser to URL for last build results.

    opentest
      Opens browser to URL for last test results.

    opencoverage
      Opens browser to URL for last coverage results.

## Common Flags
    -p <project>[:<win>]
       Many commands accept a -p flag to specify the project and optional win
       to run the command against. If used, this must come before other cmd
       args. When used the current project will not be changed, instead the
       command will be sent over TMUX to the default (first window) associated
       with the project.

# Environment Variables
## General Project Settings
    PROJECT_NAME
       Project name.

    PROJECT_TYPE
      Type of project (scala, java, go, etc).

    PROJECT_HOST
      Server project should be built/run/etc from (localhost, myserver, etc).

    DEFAULT_PROJECT_HOST
      Default project server to use if PROJECT_HOST not set.

    PROJECT_WINDOWS
      CSV list of TMUX windows to create when project session started
      (e.g. "vim,bash,repl").

    DEFAULT_PROJECT_WINDOWS
      Default list of TMUX windows to use if PROJECT_WINDOWS not set.

    PROJECT_BUG
      Current bug ID for work being done in project.

    PROJECT_DIR
      Main project directory

    PROJECT_PKGS
      Space separated list of packages for project (e.g. "com/a com/b").

    PROJECT_SRC_DIR
      Source directory for project (e.g. $PROJECT_DIR/src).

    PROJECT_TEST_DIR
      Test directory for project (e.g. $PROJECT_DIR/test).

    PROJECT_BUILD_DIR
      Directory to use when running builds or linting.

    PROJECT_BIN_DIR
      Directory containing executable program(s).

    PROJECT_GEN_DIR
      Directory containing generated code.

    PROJECT_DEFAULT_FORMAT_TARGETS
      Default targets to use when format called without parameters. (e.g.
      "FooClass BarClass"). If not set, a recursive target of '...' is added
      from $PROJECT_SRC_DIR and $PROJECT_TEST_DIR for each package listed in
      $PROJECT_PKGS.

    PROJECT_DEFAULT_LINT_TARGETS
      Default targets to use when lint called without parameters. (e.g.
      "FooClass BarClass"). If not set, a recursive target of '...' is added
      from $PROJECT_SRC_DIR and $PROJECT_TEST_DIR for each package listed in
      $PROJECT_PKGS.

    PROJECT_DEFAULT_BUILD_TARGETS
      Default targets to use when build called without parameters. (e.g.
      "FooClass BarClass"). If not set, a recursive target of '...' is added
      from $PROJECT_SRC_DIR and $PROJECT_TEST_DIR for each package listed in
      $PROJECT_PKGS.

    PROJECT_DEFAULT_TEST_TARGETS
      Default targets to use when test called without parameters. (e.g.
      "FooTest BarTest"). If not set, a recursive target of '...' is added
      from $PROJECT_TEST_DIR for each package listed in $PROJECT_PKGS.

    PROJECT_TARGETS_IGNORE
      This variable is set to a comma separated list of names matching
      targets to ignore when searching for matches for build/lint/...
      (e.g. /tmp, /backup, etc). Any matching occurrence of these tags
      within a target will match (e.g. /tmp means */tmp*)

    DEFAULT_PROJECT_TARGETS_IGNORE
      Default targets to ignore when PROJECT_TARGETS_IGNORE not set.

    PROJECT_DEFAULT_RUN_CMDS
      Commands to use when run called. A single command can be specified or a
      list of commands can used by lablling them as label::<cmd>
      (e.g. foo::foo --flags bar::bar --other_flags).

## Project Function Implementations
    PROJECT_SEARCH_FN
      Name of function to call when 'search' invoked within the project.

    DEFAULT_PROJECT_SEARCH_FN
      Default function to use if PROJECT_SEARCH_FN not set.

    PROJECT_FORMAT_FN
      Name of function to call when 'format' invoked within the project.

    DEFAULT_PROJECT_FORMAT_FN
      Default function to use if PROJECT_FORMAT_FN not set.

    PROJECT_LINT_FN
      Name of function to call when 'lint' invoked within the project.

    DEFAULT_PROJECT_LINT_FN
      Default function to use if PROJECT_LINT_FN not set.

    PROJECT_BUILD_FN
      Name of function to call when 'build' invoked within the project.

    DEFAULT_PROJECT_BUILD_FN
      Default function to use if PROJECT_BUILD_FN not set.

    PROJECT_TEST_FN
      Name of function to call when 'test' invoked within the project.

    DEFAULT_PROJECT_TEST_FN
      Default function to use if PROJECT_TEST_FN not set.

    PROJECT_COVERAGE_FN
      Name of function to call when 'coverage' invoked within the project.

    DEFAULT_PROJECT_COVERAGE_FN
      Default function to use if PROJECT_COVERAGE_FN not set.

    PROJECT_SANITY_FN
      Name of function to call when 'sanity' invoked within the project.

    DEFAULT_PROJECT_SANITY_FN
      Default function to use if PROJECT_SANITY_FN not set.

    PROJECT_GETERRORS_FN
      Name of function to call when 'geterrors' invoked within the project.
      Errors should be returned as lines of: <file>:<line>:<col>:<message>.
      See geterrors function definition for more information.

    DEFAULT_PROJECT_GETERRORS_FN
      Default function to use if PROJECT_GETERRORS_FN not set.

    PROJECT_GETURLS_FN
      Name of function to call when 'geturls' invoked within the project.

    PROJECT_DEFAULT_GETURLS_FN
      Default function to use if PROJECT_GETURLS_FN not set.

## Sharing/Syncing/Backup/...
    PROJECT_SHARE_DIR
      This variable is set to the directory to use for copying executables
      for sharing publically (e.g. ~/Public).

    DEFAULT_PROJECT_SHARE_DIR
      Default to use when PROJECT_SHARE_DIR not set.

    PROJECT_BACKUP_DIR
      This variable is set to the directory to use for project backups when
      the 'project backup' command is used.

    DEFAULT_PROJECT_BACKUP_DIR
      Default to use when PROJECT_BACKUP_DIR not set.

    PROJECT_SYNC_LIST
      This variable is set to the name of a file containing a list of files
      that should be synchronized from the local host to the project host
      when using 'project sync' command. Listed files are specified relative
      to the local home directory.

    DEFAULT_PROJECT_SYNC_LIST
      Default to use when PROJECT_SYNC_LIST not set. By default this is called
      ~/.projectsync which contains ~/.projects.

    PROJECT_SYNC_DESTS
      Comma separated list of directories to sync files to on remote host.

    DEFAULT_PROJECT_SYNC_DESTS
      Default to use when PROJECT_SYNC_DESTS not set. Typically this will just
      be ~.

## Misc
    PROXY_CMDS
      Set to true to proxy commands to remote host when running locally. If not
      set, you must explicity attach to remote TMUX session to run commands.

    VERBOSE
      Some commands take a VERBOSE environment variable. This is a level setting
      not on/off. Level 0 effectively means off which is different than most
      flags in bash where 0 is success). Many commands do not support verbose
      output yet, so don't expect much.

    DRY_RUN
      The dry run flag will print the command that will execute, but not
      actualy execute it. This is a true/false settings (e.g. DRY_RUN=true)

# Implementing Project Functions

## Helper Functions

    _normalize_targets <targets>
      Converts a list of targets relative to cur directory to names relative
      to the base source or test directory.

      Example (assuming cur dir /home/dude/proj/src/a):
        $ normalized=$(_normalize_targets a1 ../b/...)
        $ echo $normalized
        a/a1 b/...

    _expand_targets <extension> <targets>
      Converts a list of targets containing the recursive '...' indicator into
      full paths ending in the given extension.

      Example (assuming dirs a/a1, a/a2, b/b2):
        $ expanded=$(_expand_targets *.scala a/... x.txt b/...)
        $ echo $expanded
        a/a1/*.scala a/a2/*.scala x.txt b/b2/*.scala

    _print_cmd <heading> <cmd> <targets>
      Pretty prints information about a command being run.

      Example
        $ _print_cmd "ECHO" "echo" $expanded
        ECHO ----------------------------
        PWD: /home/dude/proj
        CMD: echo a/a1/*.scala \
                  a/a2/*.scala \
                  x.txt \
                  b/b2/*.scala
        ---------------------------------

## Example Implementation

    # Example: 'format ../...' => gofmt -w a/* a/a1/*.go a/a2/*.go b/*.go
    #   Assuming in src/a and there are src/a/a1, src/a/a2, src/b dirs
    function example_format() {
      # convert to paths based on src or test dir
      local targets=$(_normalize_targets "$@")

      # replace '...' with recursive dir/*.go
      local expanded=$(_expand_targets "/*.go" ${targets})

      local cmd="gofmt -w"

      # print what will be run
      _print_cmd "FORMATTING" "$cmd" ${expanded}

      # run the format
      $cmd ${expanded}
    }

# License

Copyright 2012 Mike Dreves

All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v1.0
which accompanies this distribution, and is available at:

    http://opensource.org/licenses/eclipse-1.0.php

By using this software in any fashion, you are agreeing to be bound
by the terms of this license. You must not remove this notice, or any
other, from this software. Unless required by applicable law or agreed
to in writing, software distributed under the License is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied.
