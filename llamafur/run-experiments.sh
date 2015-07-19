# The current directory should look like this:
# ./stopwords.txt                           # <---- stopwords, one per line
# ./dump
# ./dump/enwiki-20140203-pages-articles.xml # <---- the wikipedia dump
# ./run-experiments.sh                      # <---- this script
# ./evaluation
# ./evaluation/pool.txt                     # <---- specifies which scorers should be evaluated
# ./evaluation/human-evaluation.tsv         # <---- the human-made dataset

WIKIDUMP_XML=dump/enwiki-20140203-pages-articles.xml
STOPWORDS=stopwords.txt
N_TOP_CATEGORIES=20000
SEED=1234567890





############### PARSING WIKIPEDIA DUMP #########################################

# Generate page names, category names, page to category map, category pseudotree 
java efen.parsewiki.WikipediaCategoryProducer $WIKIDUMP_XML ./

# precomputation steps needed for the graph
mkdir resolvers
java it.unimi.di.big.mg4j.tool.URLMPHVirtualDocumentResolver -o pages.uris resolvers/enwiki.vdr
java efen.parsewiki.WikipediaDocumentSequence $WIKIDUMP_XML http://en.wikipedia.org/wiki/ pages.uris resolvers/enwiki.vdr resolvers/enwikired.vdr
# produce the actual graph
java efen.parsewiki.WikipediaGraphProducer $WIKIDUMP_XML resolvers/enwikired.vdr pages-with-selfloop
# remove self-loops from graph
java it.unimi.dsi.webgraph.Transform arcfilter pages-with-selfloop pages NO_LOOPS
rm pages-with-selfloop*

# Generate the Archive4j archive with textual data for text-based scorers
mkdir text-archive
cd text-archive
java efen.parsewiki.WikipediaTextArchiveProducer ../$WIKIDUMP_XML ../$STOPWORDS Archive
cat Archive.terms | java it.unimi.dsi.util.FrontCodedStringList Archive.termmap.inverted
cd ..





############### SELECTING TOP CATEGORIES #######################################
mkdir top-categories
cd top-categories
java efen.categories.CategorySelectionToolchain ../categoryPseudotree.graph ../page2cat.ser $N_TOP_CATEGORIES --names ../catId2Name.ser top -e "wiki" -e "categories" -e "main topic classifications" -e "template" -e "navigational box" --trim
cd ..



############### LEARNING LLAMAFUR MATRIX #######################################
mkdir llamafur
cd llamafur
java efen.scorers.llamafur.LatentMatrixEstimator ../top-categories/top-page2cat.ser ../pages.graph llamafur-w --seed $SEED -k 2 -m $N_TOP_CATEGORIES --savestats accuracy-recall.tsv
java efen.scorers.llamafur.NaiveMatrixEstimator ../top-categories/top-page2cat.ser ../pages.graph naive-llamafur-w.ser
cd ..

### computing stats
#cd evaluation
#java efen.scorers.ScorerStatisticsSummarizer "scorers.aa.AdamicAdarScorer(../pages)" AdamicAdar-stats.properties
#java efen.scorers.ScorerStatisticsSummarizer "scorers.textual.JacquenetM4Scorer(../pages, ../text-archive/Archive.archive)" JacquenetM4-stats.properties
#java efen.scorers.ScorerStatisticsSummarizer "scorers.llamafur.LlamaFurScorer(../pages, ../llamafur/llamafur-w-1.ser, ../top-categories/top-page2cat.ser, LlamaFur)" LlamaFur-stats.properties
#cd ..


############### EVALUATING RESULTS #############################################
cd evaluation
java efen.scorers.llamafur.classifier.evaluation.TestMatrix 200 10000 ../llamafur/llamafur-w-1.ser ../pages.graph ../top-categories/top-page2cat.ser 1 | tee >(grep -v INFO > test-matrix.txt)
java efen.evaluation.createdataset.PooledDatasetChecker pool.txt ../pageName2Id.ser human-evaluation.tsv | tee >(grep -v INFO > dataset-check.txt)
java efen.evaluation.measure.BPrefMeasure pool.txt human-evaluation.tsv ../pageName2Id.ser -o bprefs.tsv -n ../pageId2Name.ser | tee >(grep -v INFO > avg-bpref.txt)
java efen.evaluation.measure.PrecisionRecallPlot pool.txt human-evaluation.tsv ../pageName2Id.ser -o precision-recall.tsv -n ../pageId2Name.ser
java efen.analysis.ScorerComparison ../pageName2Id.ser human-evaluation.tsv llamafur-vs-human.tsv "scorers.llamafur.LlamaFurScorer(../pages, ../llamafur/llamafur-w-1.ser, ../top-categories/top-page2cat.ser, LlamaFur)"
cd ..
