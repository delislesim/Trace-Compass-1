/*******************************************************************************
* Copyright (c) 2015 Efficios Inc.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Jonathan Rajotte Julien - initial implementation
*******************************************************************************/
:
# tracecompass-rcptt-test
Blackbox UI test using RCPTT for Trace Compass

## How to run ?

You can run this project either via the RCP Testing Tool or via maven.

### Local maven run
By default maven will look for the RCP under ./rcp/trace-compass .

This can be overridden by providing a -DauthPath="/path/to/rcp/"

```
mvn package -DautPath="/tmp/rcp/trace-compass/"
```

By default maven will lokk for Data in ./Data .
This can be overridden by providing a -DdataPath="/path/to/data/folder"
```
mvn package -DdataPath="/tmp/data/"
```

Both argument can be combined.
```
mvn package -DautPath="/tmp/rcp/trace-compass/" -DdataPath="/tmp/data"
```

Maven will take care of all necessary dependencies and run the pre-selected test
suites.

Results will be located under ./target.

### Via RCPTT

Add an AUT (Application Under Test) [More detail here](https://www.eclipse.org/rcptt/documentation/userguide/getstarted/).

If you are not using the default path make sure to pass the correct arguments to
the AUT in the run configurations.

![](http://i.imgur.com/J4ohsPE.png)

## Development

Two main set of tests exists: static and dynamic traces based tests.

Tests based on dynamic traces require that tests do not know anything about the
trace content.

Trace place holder for dynamic traces need to be present under data to provide a fully
running project.

Tests based on static traces can test things related to the trace content.
Naturally traces needed for static tests must be either present under data or
downloaded on run from a remote site.
