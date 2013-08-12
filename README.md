# Overview

Projux is a set of BASH scripts that provide command line support for
managing one or more project working directories and related settings.
Projects can be managed both locally and remotely (via TMUX).

Built-in support is provided for connecting to remote workspaces, switching
between workspaces (automatically setting and clearing project environment variables), and managing sessions. A framework is also provided for
implementing project specific functions for formatting, linting, building,
testing, etc based on a standard set of commands common to all projects.

# Installation

Installation is straightforward:

1. Clone the repository to a local machine (e.g. Mac laptop, etc):

        cd ~
        git clone git://github.com/mdreves/projux.git

2. Add default project settings to `~/.bashrc` and source `.projuxrc`

        if [ -f .projux/.projuxrc ]; then
          # default settings
          ...

          . .projux/.projuxrc
        fi

    Note that the default settings must be provided before the `.projuxrc`
    file is sourced. See `.projux/.sample-bashrc` for an example setup.

3. Add per project settings to `~/.projects`

    See `.projux/.sample-projects` for an example setup.


4. Setup remote hosts (if needed)

    This can either be done using `project sync` or manually.  In either case,
    make sure to set either the `DEFAULT_PROJECT_HOST` var in `~/.bashrc` (if
    hosting all projects on a single server) or per project `PROJECT_HOST` vars
    in `~/.projects` (if different projects have different servers).

    a) Manually

    To setup manually, copy the `~/.projux` dir and `~/.projects` file to the
    remote host(s) and setup the remote `~/.bashrc` file as per step 2 and 4.

    b) Using sync

    If all the projects are hosted on a single development server, then in
    addition to the `DEFAULT_PROJECT_HOST` variable set in `~/.bashrc`, add
    a `DEFAULT_PROJECT_SYNC_LIST` variable pointing to a file that includes
    at least the `.projux` dir and the `.projects` file. If you are like me
    and have a shared `~/.bashrc` file used between a Mac laptop and a linux
    desktop server, then you can also add `.bashrc`. If not, you will need to
    manually setup the remote `.bashrc` as per step 2 and 4. If different
    projects are hosted on different servers, then you will need to setup
    project specific `PROJECT_SYNC_LIST` variables within the '.projects'
    file (similar to the `PROJECT_HOST` variables). There are examples for
    settings in `.projux/.sample-bashrc`, `.projux/.sample-projects`, and
    `.projux/.sample-projectsync`.

    Now, synchronize the local and remote host(s):

    If using `DEFAULT_PROJECT_HOST` and `DEFAULT_PROJECT_SYNC_LIST` settings:

        $ project sync

    If separate `PROJECT_HOST` and `PROJECT_SYNC_LIST` settings:

        $ project sync <project_name>

# Example

The best way to understand how projux works is to go through a detailed
example. Let's assume we have a two projects called `foo` and `bar`. Let's
further assume that `foo` is a Scala based project while `bar` is a mix of
C++ and Go. We will also assume that all our projects are hosted on a shared
remote server called `devhost` that is used for development. The first thing
we need to do is setup `~/.bashrc` to work with our remote server and our
development languages.  Here is an example of the settings we might add to
`~/.bashrc`:

        # devhost is our shared development server
        export DEFAULT_PROJECT_HOST=devhost

        # Setup TMUX windows for vim (bash), building (bash), and a repl (scala)
        export DEFAULT_PROJECT_WINDOWS="\
          vim::bash \        # vim window runs in bash shell
          build::bash \      # build window runs in a bash shell
          repl::scala"       # repl window runs scala interpreter

        # By default don't include any files in dirs with names tmp or backup
        export DEFAULT_PROJECT_EXCLUDE_FILTER="*tmp/*:*backup/*"

        # Format implementations for C++ and Go files
        export DEFAULT_PROJECT_FORMAT_CMDS="\
          c:cc:cpp:h::clang-format -i <targets> \
          go::gofmt -tabs=false -tabwidth=2 -w <targets>"

        # Lint implementation for Go files (using both govet and golint)
        export DEFAULT_PROJECT_LINT_CMDS="\
          go::golint <targets>; govet <targets>"

        # Build implementation for Scala, C++, and Go files
        export DEFAULT_PROJECT_BUILD_CMDS="\
          scala::scalac <targets> \
          cc::clang++ -std=c++11 -stdlib=libc++ -W <targets> \
          go::go <targets>"

We could also add support for test, coverage, clean, package, etc, but we
will keep the example simple for now.

Now we need to add our project specific settings to `~/.projects`. Environment
variables and aliases are associated with a given project based on their
position within the file.  All variables and aliases from the start of a
`PROJECT_NAME` entry up to the next `PROJECT_NAME` entry will be associated
with the same project. These variables/alaises are set whenever the
`project <project_name>` command is used.

        # foo project
        PROJECT_NAME=foo
        PROJECT_INCLUDE_FILTER=*.scala
        PROJECT_DIR=${HOME}/Workspace/${PROJECT_NAME}
        PROJECT_SRC_DIR=${PROJECT_DIR}/src
        PROJECT_TEST_DIR=${PROJECT_DIR}/test
        PROJECT_TARGET_DIR=${PROJECT_DIR}/target
        PROJECT_BIN_DIR=${PROJECT_TARGET_DIR}/bin
        PROJECT_PKGS="com/xxx/foo com/xxx/common"
        PROJECT_DEFAULT_RUN_CMDS="\
            client::client -log_dir=log -server=localhost:54321 \
            server::server -port=54321"
        alias start="startup_script"
        alias stop="stop_script"

        # bar project
        PROJECT_NAME=bar
        PROJECT_INCLUDE_FILTER=*.{cc,h}:*.go
        PROJECT_DIR=${HOME}/Workspace/${PROJECT_NAME}
        PROJECT_SRC_DIR=${PROJECT_DIR}/src
        PROJECT_TEST_DIR=${PROJECT_SRC_DIR}
        PROJECT_TEST_SUFFIXES="_test"
        PROJECT_TARGET_DIR=${PROJECT_DIR}/target
        PROJECT_BIN_DIR=${PROJECT_TARGET_DIR}/bin
        PROJECT_PKGS="xxx/bar"
        PROJECT_DEFAULT_RUN_CMDS="bar --echo_flag="hello"

At this point we will assume that the projects have been syncd between the
local and remove hosts using `project sync`, etc as described in the
installation steps. We can now attach to a project in a remote workspace using:

        $ project attach foo

The first time this command is run from the local host it will create all the
TMUX windows listed in `DEFAULT_PROJECT_WINDOWS` and will then attach to the
first window listed (`vim` in our example). Subsequent calls will then reattach
wherever you left off.

We could attach another terminal window by issuing the same command in a
different terminal, but doing will cause all our actions in one window to
be mirrored in the other (including switching windows). This may be useful
in some cases (e.g. mirroring a session so one person can watch what another
person is doing), but in our case we would like to attach a second terminal to
a different window. Let's assume we are running on Mac and are using iTerm2 to
create a split view of two terminal windows side by side. In the first terminal
we would like to run vim under project `foo` and in the second terminal we would
like to run builds (also under project `foo`). To do this, we need to use TMUX
grouped sessions. The setup is a bit complicated, but projux let's us do this
by just adding a number (2 in this case) to the end of the command to have it
work in another session group. In the second terminal we will run:

        $ project attach foo 2

This will load the same project environment variables as the first terminal,
but instead of attaching to the first window by default, it will attach to the
second TMUX window (`build` in our example). These terminals can now operate
independently. Using the `win` command (or TMUX itself) you can switch the
second terminal to the first TMUX window (in which case you will get mirroring
between the terminals), but switching out of that window to another will only
effect the second terminal window and not the first (e.g. TMUX window switches
are now per terminal). If you looked at the tmux session itself, you would see
that it contains '_2' in the name (e.g.  foo_2). I recommend associating a
number with a terminal and operating on all projects within that terminal using
the same number.

Now lets switch projects. To change the first terminal window to load project
`bar` use the following command:

        $ project bar

Notice that we didn't use attach since we are already attached to the remote
server so this command is being run remotely. We can switch the second terminal
window to bar as well by using:

        $ project bar 2

An you can go back and forth.

        $ project foo

Once you are working with a given project, all the environment variables and
aliases will be set to that project. Each time you switch, you clear the
previous and load the new. You can read more in the docs below, but there are
various subcommands supported by project such as listing existing projects,
killing project sessions, etc. Note that the namespace is overloaded, so in the
unlikley case that you call you project 'ls', etc, you may run into issues...

In addition to project management related commands, projux provides support
for common development related operations. For example, to format all the files
(that have formatting implementations) use the following:

        $ format ...

Based on our settings, this command would only work in the `bar` project because
a format implementation was not provided for `foo`. A full list of commands is
provided in the documentation below, but it covers basic things such as lint,
build, test, etc. It also covers commands for running a built executable:

        $ run                        # runs 'client -log_dir=log -port=54321'
        $ run client -log_dir=/tmp   # runs 'client -log_dir=/tmp -port=54321'
        $ run server                 # runs 'server -port=54321'

When you need to disconnect from the development server, just issue the
following command:

        $ project detach

When you call attach next time it will pick back right were you left off.

# Project Management Commands

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
        project attach 2       :  Attach/switch to 2nd session for project
        project attach foo 3   :  Attach/switch to 3rd session for project foo

    $ project detach [<project>] [<session>]

      Detach from TMUX session.

      Examples:
        project detach         :  Detach current client from cur project
        project detach foo     :  Detach all clients attached to foo project
        project detach foo 2   :  Detach all clients attached to 2nd foo session

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

    $ project clear

      Clear env var settings for current project.

      Examples:
        project clear

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

# Project Specific Bash Commands
## pssh
    ssh -Y -t $PROJECT_HOST

## psftp
    sftp $PROJECT_HOST

## pvim
    vim --servername $PROJECT_NAME -c ":Open :session"

# Project Development Commands

## Overview

### Recursive Targets

The characters `...` following a directory target means the directory and all
sub-directores recursively. For example, `build a/...` means build the
`a/` directory and all its sub-directories.

### Keywords

By convention, keyword flags are prefixed with `:` (e.g. `geterrors :build`).

### Common Flags
    -p <project>[:<win>]
       Many commands accept a -p flag to specify the project and optional win
       to run the command against. If used, this must come before other cmd
       args. When used the current project will not be changed, instead the
       command will be sent over TMUX to the default (first window) associated
       with the project.

## format
    $ format [-p <project>] <targets>

      Runs project specific format based on $PROJECT_FORMAT_CMDS.

      Examples:
        format foo.cc          :  Format specific file
        format                 :  Format recursively from current dir
        format ...             :  As above
        format :project        :  Format all project files
        format -p foo ...      :  Format recursively in project foo

## lint
    $ lint [-p <project>] <targets>

      Runs project specific lint based on $PROJECT_LINT_CMDS.

      Examples:
        lint foo.py            :  Lint specific file.
        lint                   :  Lint recursively from current dir
        lint ...               :  As above
        lint :project          :  Lint all project files
        lint -p foo ...        :  Lint recursively in project foo

## build
    $ build [-p <project>] <targets>

      Runs project specific build based on $PROJECT_BUILD_CMDS.

      Examples:
        build Foo.scala        :  Build specific file.
        build                  :  Build recursively from current dir
        build ...              :  As above
        build -p foo ...       :  Build recursively in project foo

## test
    $ test [-p <project>] <targets>

      Runs project specific test based on $PROJECT_TEST_CMDS.

      Examples:
        test my_test           :  Test specific file.
        test                   :  Test recursively from current dir
        test ...               :  As above
        test -p foo ...        :  Test recursively in project foo

    $ gettest [-p <project>]

      Returns project default test targets ($PROJECT_TEST_TARGETS).

    $ settest [-p <project>] <targets>

      Sets project default test targets ($PROJECT_TEST_TARGETS).

      Examples:
        settest Foo            :  PROJECT_TEST_TARGETS=Foo

    $ updatetest [-p <project>] {+,-}<targets>

      Add/remove to/from project default test targets ($PROJECT_TEST_TARGETS).

      Examples:
        updatetest +foo        :  Add foo to PROJECT_TEST_TARGETS
        updatetest -bar        :  Remove bar from PROJECT_TEST_TARGETS

    $ cleartest [-p <project>]

      Clears project default test targets ($PROJECT_TEST_TARGETS).

## coverage
    $ coverage [-p <project>] <targets>

      Runs project specific coverage based on $PROJECT_COVERAGE_CMDS.

      Examples:
        coverage my_test       :  Coverage for a specific file.
        coverage               :  Coverage recursively from current dir
        coverage ...           :  As above
        coverage -p foo ...    :  Coverage recursively in project foo

## clean
    $ clean [-p <project>]

      Cleans project temp files based on $PROJECT_CLEAN_CMDS.

      Examples:
        clean                  :  Clean project files
        clean -p foo           :  Clean project foo

## run
    $ run [-p <project>] [<label>] [<flags>]

      Runs project specific program based on $PROJECT_RUN_CMDS setting.
      Multiple executables can be tagged with 'key::' labels to distinguish
      them. The key label is then provided to the run command to select the
      appropriate program. If no label is provided then the first entry is used.

      Examples:
        PROJECT_RUN_CMDS="client::client --f=a server::server --port=6"
        run                    :  $PROJECT_RUN_DIR/client --f=a
        run client             :  As above
        run client --f=-a,+b   :  $PROJECT_RUN_DIR/client --f=b
        run client --f=c       :  $PROJECT_RUN_DIR/client --f=c
        run shared server      :  $PROJECT_SHARE_DIR/server --port=6

    $ getrun [-p <project>] [<label>] [<flags>]

      Returns project run commands ($PROJECT_RUN_CMDS).

      Examples:
        PROJECT_RUN_CMDS="client::client --f=a server::server --port=6"
        getrun                 : client::client --f=a server::server --port=6
        getrun client          : client --f=a
        getrun client --f=+b   : client --f=a,b

    $ setrun [-p <project>] <cmds>

      Sets project run commands ($PROJECT_RUN_CMDS).

      Examples:
        setrun client::client --f=a server::server --port=6

    $ updaterun [-p <project>] <label> <flags{+,-}>

      Add/remove flags to/from a project run command ($PROJECT_RUN_CMDS).

      Examples:
        updaterun client::client --f=-a,+b

    $ clearrun [-p <project>]

      Clears project run commands ($PROJECT_RUN_CMDS).

## share
    $ share [-p <project>] <prog>

      Copies named program file from $PROJECT_BIN_DIR to $PROJECT_SHARE_DIR.

      Examples:
        share client           :  cp $PROJECT_BIN_DIR/client $PROJECT_SHARED_DIR

## package
    $ package [-p <project>] [<label>] [<flags>]

      Package project output files based on $PROJECT_PACKAGE_CMDS.

      Examples:
        package                :  Package project output
        package -p foo         :  Package project foo

## deploy
    $ deploy [-p <project>] [<label>] [<flags>]

      Deploy project based on $PROJECT_DEPLOY_CMDS.

      Examples:
        deploy                 :  Deploy project
        deploy -p foo          :  Deploy project foo

## gendocs
    $ gendocs [-p <project>] [<label>] [<flags>]

      Generate docs for project based on $PROJECT_GENDOCS_CMDS.

      Examples:
        gendocs                :  Generate docs for project
        gendocs -p foo         :  Generate docs for project foo

## search
    $ search [-p <project>] [<label>] [<patterns>]

      Runs project specific search based on $PROJECT_SEARCH_CMDS.

      Examples:
        search xxx             :  Search for xxx in project code base
        search -p foo xxx      :  Search for xxx in project foo code base

## sanity
    $ sanity [-p <project>]

      Runs project specific sanity. The default implementation runs format,
      lint, build, test, and coverage. A project specific implementation can
      be defined by setting $PROJECT_SANITY_FN.

      Examples:
        sanity                 :  Run sanity for current project
        sanity -p foo          :  Run sanity on project foo

## Misc
    $ geterrors {:build|:lint|:test|:coverage}

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

    geturl {:build|:test|:coverage|:bug <id>|:review <id>}
      Returns URL with results for last build/test/coverage or for a bug/review.

    openbug <id>
      Opens browser to URL for bug <id>.

    openreview <id>
      Opens browser to URL for review <id>.

    openbuild
      Opens browser to URL for last build results.

    opentest
      Opens browser to URL for last test results.

    opencoverage
      Opens browser to URL for last coverage results.

# Project Aliases
    cdproject
      cd $PROJECT_DIR

    pushproject
      pushd $PROJECT_DIR

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

    catlast
      cat $PROJECT_LAST_OUT_DIR/last_run

    vilast
      vi $PROJECT_LAST_OUT_DIR/last_run

    catlint
      cat $PROJECT_TARGET_DIR/lint/last_run

    vilint
      vi $PROJECT_TARGET_DIR/lint/last_run

    catbuild
      cat $PROJECT_TARGET_DIR/build/last_run

    vibuild
      vi $PROJECT_TARGET_DIR/build/last_run

    cattest
      cat $PROJECT_TARGET_DIR/test/last_run

    vitest
      vi $PROJECT_TARGET_DIR/test/last_run

    catcoverage
      cat $PROJECT_TARGET_DIR/coverage/last_run

    vicoverage
      vi $PROJECT_TARGET_DIR/coverage/last_run

# Environment Variables

## Overview

Projux has special settings for some of the environment variables.

### Key/Value Maps

Some environment variables take a map of key/value settings. The settings are
specified using the following form:

        <key>::<value> <key>::<value> ...

The <key> and <value> settings depend on the type of environment variable.
There are three main types that use this format:

1 Project Windows

In this case the <key> is the window name and the <value> is the command to
use when launching the window. For example to launch bash in a 'vim' window
and scala in a 'repl' window:

        PROJECT_WINDOWS="\
            vim::bash; stty -ixon \   # tmux new-window -d vim 'bash; stty-ixon'
            repl::scala"              # tmux new-window -d vim 'scala'

2 Environment Variables with Commands Per File Type

In this case the <key> is a colon separated list of file extensions and the
<value> is the command(s) to use on files of those types. For example, for
the PROJECT_FORMAT_CMDS we could use the following to run clang-format on
files of type *.{cc,h} and gofmt on files of type *.go, use:

        PROJECT_FORMAT_CMDS="\
            cc:h::clang-format -i <targets> \
            go::gofmt -tabs=false -tabwidth=2 -w <targets>"

In this case the keywords <args> will be replaced with the args as passed
while <target> will be replaced with the expanded targets per file type.

Multiple commands can be run at once by separating them with ';'. For
example, to run javacheck on *.java and both golint and govet on *.go files:

        PROJECT_LINT_CMDS="\
            java::javacheck <targets> \
            go::golint <targets>; govet <targets>"

3 Environment Variables with Selectable Commands

In this case the <key> represents a name that can be used to select from
one or more commands. When the associated call is made the key is passed
and the commmand(s) associated with that key is run. If no key is provided
then the first command listed is used.

        PROJECT_RUN_CMDS="\
            client::client -server=localhost:1234 -data=a,b \
            server::server -port=1234"

        $ run client    # runs client -server=localhost:1234 -data=a,b
        $ run server    # runs server -port=1234
        $ run           # runs client -server=localhost:1234 -data=a,b

### Selectable Commands

As mentioned, selectable commands allow you to select from one or more
commands based on a key:: tag. However, you can also modify the flags that
are associated with the selected command.

        $run server --port=5678       # runs 'server -port=5678'
        $run client --data=+c         # runs 'server -port=1234 -data=a,b,c'
        $run client --data=-b,+e      # runs 'server -port=1234 -data=a,e'

### Defaults

An environment variable `XXX` can have a default called `DEFAULT_XXX`. The
`DEFAULT_XXX` value is used when a project does not set a specific `XXX` value.
`XXX` variables are set in `~/.projects` while `DEFAULT_XXX` are set in
`~/.bashrc`.

## General Project Settings
    PROJECT_NAME (required)
       Project name.

    PROJECT_HOST / DEFAULT_PROJECT_HOST
      Server project should be built/run/etc from (localhost, myserver, etc).

    PROJECT_WINDOWS / DEFAULT_PROJECT_WINDOWS
      Map of tmux windows. The keys are window names and the values are
      commands to use when the window is opened (see overview).
      (e.g. "bash::bash; stty -ixon repl::python").

    PROJECT_DIR (required)
      Main project directory

    PROJECT_SRC_DIR (required)
      Source directory for project (e.g. $PROJECT_DIR/src).

    PROJECT_TEST_DIR (required)
      Test directory for project (e.g. $PROJECT_DIR/test).

    PROJECT_TARGET_DIR (required)
      Directory for build/lint output (e.g. $PROJECT_DIR/target)

    PROJECT_BIN_DIR (required)
      Directory containing executable program(s) (e.g.
      $PROJECT_TARGET_DIR/bin). PROJECT_RUN_CMDS are relative to this
      directory.

    PROJECT_GEN_DIR (required if cdgen used)
      Directory generated code output to (e.g. $PROJECT_TARGET_DIR/gen).

    PROJECT_PKGS (required)
      List of main packages used by project (e.g. "com/example1 com/example2").
      These are directories that should exist in the PROJECT_SRC_DIR and/or
      PROJECT_TEST_DIR. Default is all directories under source and test dirs.

    PROJECT_INCLUDE_FILTER / DEFAULT_PROJECT_INCLUDE_FILTER (required)
      Colon separated list of patterns for files that should be considered part
      of the project (for example, make:*.scala:*.java:*.go:*.{c,cc,h}). When
      format, lint, build, test, etc are run, these files are searched for in
      the PROJECT_SRC_DIR and/or PROJECT_TEST_DIR directories. Multiple file
      types may be specified even though only some operations are supported on
      some files (e.g. lint may only be supported for *.java and although
      *.scala may be included in the filter, lint will only be run for java).
      It is best to group files together that go together (e.g. *.{c,cc,h} vs
      *.c:*.cc:*.h). This setting is required.

    PROJECT_EXCLUDE_FILTER / DEFAULT_PROJECT_EXCLUDE_FILTER
      Colon separated list of patterns for files that should be NOT be
      considered part of the project (for example, Foo.scala:Bar.java). If these
      files exist in the PROJECT_SRC_DIR or PROJECT_TEST_DIR they will be
      ignored.

    PROJECT_BUG
      Current bug ID for work being done in project.

## Setttings for Selectable Commands

    PROJECT_RUN_CMDS / DEFAULT_PROJECT_RUN_CMDS
      Command(s) to use when run called. A single command can be specified or a
      map of commands can used by labelling them as <key>::<cmd>
      (e.g. clien::client -server=localhost:1234 server::server -port=1234).

    PROJECT_CLEAN_CMDS / DEFAULT_PROJECT_CLEAN_CMDS
      Command(s) to use when clean called. A single command can be specified or
      a map of commands can used by labelling them as <key>::<cmd>
      (e.g. sbt clean).

    PROJECT_PACKAGE_CMDS / DEFAULT_PROJECT_PACKAGE_CMDS
      Command(s) to use when package called. A single command can be specified
      or a map of commands can used by labelling them as <key>::<cmd>
      (e.g. sbt package).

    PROJECT_DEPLOY_CMDS / DEFAULT_PROJECT_DEPLOY_CMDS
      Command(s) to use when deploy called. A single command can be specified or
      a map of commands can used by labelling them as <key>::<cmd>
      (e.g. frontend::ant deploy-fe backend::ant deploy-be).

    PROJECT_GENDOCS_CMDS / DEFAULT_PROJECT_GENDOCS_CMDS
      Command(s) to use when gendocs called. A single command can be specified
      or a map of commands can used by labelling them as <key>::<cmd>
      (e.g. scala::sbt doc go::godocs --http=:6060).

    PROJECT_SEARCH_CMDS / DEFAUT_PROJECT_SEARCH_CMDS
      Command(s) to use when search called. A single command can be specified
      or a map of commands can used by labelling them as <key>::<cmd>
      (e.g. default::mysearch -src=${PROJECT_SRC_DIR}).

## Settings for Commands Based on File Types

    PROJECT_FORMAT_CMDS / DEFAULT_PROJECT_FORMAT_CMDS
      Map of commands to run when formatting project files. The map should
      have the form: <file_ext>:...<file_ext>::<cmd>;...;<cmd> ...
      (e.g. "cc:h::clang-format -i <targets> go::gofmt -w <targets>")

    PROJECT_LINT_CMDS / DEFAULT_PROJECT_LINT_CMDS
      Map of commands to run when linting project files. The map should
      have the form: <file_ext>:...<file_ext>::<cmd>;...;<cmd> ...
      (e.g. "cc:h::clint <targets> go::golint <targets>; govet <targets>")

    PROJECT_BUILD_CMDS / DEFAULT_PROJECT_BUILD_CMDS
      Map of commands to run when compling project files. The map should
      have the form: <file_ext>:...<file_ext>::<cmd>;...;<cmd> ...
      (e.g. "cc:h::make scala::scalac <targets> java::javac <targets>")

    PROJECT_TEST_CMDS / DEFAULT_PROJECT_TEST_CMDS
      Map of commands to run when testing project files. The map should
      have the form: <file_ext>:...<file_ext>::<cmd>;...;<cmd> ...
      (e.g. "scala::sbt test <args>")

    PROJECT_COVERAGE_CMDS / DEFAULT_PROJECT_COVERAGE_CMDS
      Map of commands to show code coverage of project tests. The map should
      have the form: <file_ext>:...<file_ext>::<cmd>;...;<cmd>,...
      (e.g. "rb::coverage <args>")

    PROJECT_TEST_SUFFIXES / DEFAULT_PROJECT_TEST_SUFFIXES
      Map of suffixes used on test related files. This is used to distinguish
      source and test files when they reside in the same directory. The map
      should have the form: <file_ext>:...<file_ext>::<suffix> ...
      (e.g. "cc::_test")

    PROJECT_TEST_TARGETS
      Override for test/coverate include filters. When set
      PROJECT_INCLUDE_FILTER is set to PROJECT_TEST_TARGETS before test or
      coverage is called. When not set, then PROJECT_INCLUDE_FILTER is used as
      set. This is used with the settest, etc functions to allow configuring
      a small subset of test to run multiple times during development.

## Custom Function Implementations
    PROJECT_FORMAT_FN / DEFAULT_PROJECT_FORMAT_FN
      Name of custom 'format' function implementation.

    PROJECT_LINT_FN / DEFAULT_PROJECT_LINT_FN
      Name of custom 'lint' function implementation.

    PROJECT_BUILD_FN / DEFAULT_PROJECT_BUILD_FN
      Name of custom 'build' function implementation.

    PROJECT_TEST_FN / DEFAULT_PROJECT_TEST_FN
      Name of custom 'test' function implementation.

    PROJECT_COVERAGE_FN / DEFAULT_PROJECT_COVERAGE_FN
      Name of custom 'coverage' function implementation.

    PROJECT_GETERRORS_FN / DEFAULT_PROJECT_GETERRORS_FN
      Name of custom 'geterrors' function implementation.
      Errors should be echoed as lines of: <file>:<line>:<col>:<message>.
      See geterrors function for more information.

    PROJECT_GETURL_FN / DEFAULT_PROJECT_GETURL_FN
      Name of custom 'geturl' function implementation.

    PROJECT_SANITY_FN / DEFAULT_PROJECT_SANITY_FN
      Name of custom 'santity' function implementation.

## Sharing/Syncing/Backup/...

    PROJECT_SHARE_DIR / DEFAULT_PROJECT_SHARE_DIR
      This variable is set to the directory to use for copying executables
      for sharing publically (e.g. ~/Public).

    PROJECT_BACKUP_DIR / DEFAULT_PROJECT_BACKUP_DIR
      This variable is set to the directory to use for project backups when
      the 'project backup' command is used.

    PROJECT_SYNC_LIST / DEFAULT_PROJECT_SYNC_LIST
      This variable is set to the name of a file containing a list of files
      that should be synchronized from the local host to the project host
      when using 'project sync' command. Listed files are specified relative
      to the local home directory. The DEFAULT_PROJECT_SYNC_LIST is by default
      set to ~/.projectsync which contains ~/.projects.

    PROJECT_SYNC_DESTS / DEFAULT_PROJECT_SYNC_DESTS
      Comma separated list of directories to sync files to on remote host.

## Misc
    PROJUX_ALIASES
      Set to true to add projux related aliases (cdsrc, etc). Default is true.

    PROJUX_CMDS
      Set to true to add projux related commands (format, lint, etc). Default is
      true.

    PROXY_CMDS
      Set to true to proxy commands to remote host when running locally. If not
      set, you must explicity attach to remote TMUX session to run commands.
      Default is false.

    VERBOSE
      Some commands take a VERBOSE environment variable. This is a level setting
      not on/off. Level 0 effectively means off which is different than most
      flags in bash where 0 is success). Many commands do not support verbose
      output yet, so don't expect much. Default is true.

    DRY_RUN
      The dry run flag will print the command that will execute, but not
      actualy execute it. This is a true/false settings (e.g. DRY_RUN=true)
      Default is false.

# Implementing Custom Selectable Commands

Some projects may have their own selectable commands that fall outside of
the built-in run, search, package, etc. Projux provides a helper function to
implement these using the `__projux_select_cmd` function. For example, given the
following:

      function update() {
        # first param is environment var name, second is heading
        __projux_select_cmd "PROJECT_UPDATE_CMDS" "UPDATING" $*
      }

A project can now create a `PROJECT_UPDATE_CMDS` variable similar to
`PROJECT_RUN_CMDS` that allows this new `update` command to be called while
passing labels and flags to select the bash command that is ultimately invoked.

# License

Copyright 2012 - 2013 Mike Dreves

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
