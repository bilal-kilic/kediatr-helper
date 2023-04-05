Kediatr Helper
=======================

[![CodeFactor](https://www.codefactor.io/repository/github/bilal-kilic/kediatr-helper/badge)](https://www.codefactor.io/repository/github/bilal-kilic/kediatr-helper)
[![JetBrains Plugins](https://img.shields.io/badge/1.10.2-kediatr--helper-brightgreen)](https://plugins.jetbrains.com/plugin/16017-kediatr-helper)


<!-- Plugin description -->
**Kediatr Helper** is an Intellij Idea plugin that provides support for projects using [KediatR](https://github.com/Trendyol/kediatR)

### Features:
 - Gutter icons for command classes and commandBus executions
 - Go to handler functionality in Go To menu and gutter icons
 - Navigating to multiple handlers of a Notification
 - Navigating to handlers of inheriting types
 - Creating handlers for command types

<!-- Plugin description end -->

### Usage

#### Go to handler

Toolbar action

![Alt Text](docs/go_to_handler_toolbar.gif)

Gutter Icon

![Alt Text](docs/go_to_handler_gutter.gif)

Create Handler

![Alt_Text](docs/create_handler.gif)


### TODO

- [x] Add support for finding all handlers of notification
- [ ] Change icons to svg files
- [x] Add create handler functionality for command types
- [x] Mark types inherited from Command types
- [ ] Make project not require restart when enabling
- [ ] Make sure gutter icons only show up for function calls to CommandBus
- [ ] Show PipelineBehaviours for CommandHandlers