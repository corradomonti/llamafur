LlamaFur: Learning Latent Category Matrix to Find Unexpected Relations
======================================================================

In this repository, you will find dataset and code to be able to evaluate algorithms which aim to discover unexpected links, and to repeat LlamaFur experiments. 

The human-evaluated dataset we used to evaluate the unexpectedness of links in Wikipedia is `human-evaluation.tsv`. In this file, the first column is the title of the Wikipedia page that contains the link, the second column is the title of the target page, while the third column is `TE`, `E`, `U` or `TU` depending on whether the link is Totally Expected, Expected, Unexpected or Totally Unexpected.

The code we used is available in the directory `llamafur/java`.

# How to replicate experiments

Set up the environment
----------------------

In order to compile LlamaFur code, you'll need Java 8, Ant and Ivy. To install those inside a clean [Vagrant](http://vagrantup.com/) box with `ubuntu/trusty64`, you should use these lines:

    sudo apt-get --yes update
    sudo apt-get install -y software-properties-common python-software-properties
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
    sudo add-apt-repository ppa:webupd8team/java -y
    sudo apt-get update
    sudo apt-get --yes install oracle-java8-installer
    sudo apt-get --yes install oracle-java8-set-default
    sudo apt-get --yes install ant ivy
    sudo ln -s -T /usr/share/java/ivy.jar /usr/share/ant/lib/ivy.jar


Compile LlamaFur code 
----------------------

If the environment is set up properly, you should install git and download this repo with

	sudo apt-get install git
	git clone https://github.com/corradomonti/llamafur.git

and then go to the directory `llamafur/java`. There, run:

* `ant ivy-setupjars` to download dependencies
* `ant` to compile
* `. setcp.sh` to include the produced Jar `unexpectedness-1.0.jar` inside the Java classpath.

Please check that a command like `java efen.scorers.llamafur.LatentMatrixEstimator --help` correctly shows help.

Download the wikipedia dump
---------------------------

You should be able download the Wikipedia articles dump we used, named `	enwiki-20140203-pages-articles.xml.bz2` from [AcademicTorrents](http://academictorrents.com/details/9512a1f6d21e5012c06a1c9b8e2dd4796ecc77a9). If this torrent is not working, please email us at {`corrado.monti`, `paolo.boldi`} `@unimi.it`.
Please unzip this dump, and put it in a directory named `llamafur/dump`.


Run experiments
---------------

Then, `cd` to the `llamafur/` directory where you can proceed to run the script with `sh run-experiments.sh`. Parsing the Wikipedia snapshot will take many hours (at least 10 usually), while the LlamaFur experiments won't take as long. See the comment in the script file (`llamafur/run-experiments.sh`) for more info.
