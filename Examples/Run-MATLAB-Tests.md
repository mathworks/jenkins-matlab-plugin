# Run MATLAB Tests on Jenkins Server

This topic shows you how to run a suite of MATLAB unit tests with Jenkins. In particular, the example enables you to:

* Configure Jenkins to run a freestyle project including MATLAB tests.
* Interface Jenkins with a remote repository that contains the tests.
* Build the freestyle project and examine the test outcomes.

## Prerequisites
To run MATLAB tests stored in a remote repository, Jenkins must properly interface with MATLAB as well as the repository.
* To run MATLAB tests, you must install the MATLAB Jenkins plugin. For information on how to install a plugin in Jenkins, see [Managing Plugins](https://jenkins.io/doc/book/managing/plugins/).
* Since the tests are stored in a remote GitHub repository, you must install a command-line Git™ client. For more information, see [Getting Started - Installing Git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git).
