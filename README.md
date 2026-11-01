# Plagiarism Detection (Plagdet)

**Plagdet** is a Java-based tool for detecting plagiarism in technical documentation, that often
combines
natural-language descriptions with code and possibly pseudocode. It is designed for Slovak-language
documents and identifies potentially plagiarized passages rather than relying solely on
whole-document similarity scores.

---
The detection pipeline compares documents at the sentence level using **word alignment and neural
word embeddings**. A **JVector HNSW index** retrieves candidate sentences for detailed comparison,
with configurable retrieval limits controlling how many candidate sentences are considered.

For reference-corpus searches, **Apache Lucene** retrieves candidate documents,
whose sentences are subsequently compared using the same general approach.

The preprocessing pipeline includes sentence splitting, morphological annotation, lemmatization,
spelling correction, and code-fragment identification. Document metadata is also analyzed as an additional signal.

The tool supports **PDF, DOC, DOCX** documents, including collections organized in directories or ZIP archives.


More information about approach decision and evaluation is present in theses text, available
at [CRZP](https://opac.crzp.sk/?fn=detailBiblioForm&sid=9EB7DDC054CB7EBFE473D9EAFF98).
## Evaluation highlights 

- **75.74% F1 on a manually annotated benchmark:**
  - The best tuned configuration achieved 76.04%
    precision, 75.43% recall, and a PlagDet score of 68.58, with granularity of 1.15 (about metrics see [PAN](https://pan.webis.de/)).

- **61% lower processing time:** 
  - With code-fragment identification enabled, analysis of 981
    documents and 480,324 document pairs took 4 h 17 min, compared with 11 h 06 min without it - 
    approximately **2.6× faster** in this experiment.
  - Reason for such "radical" approach is that, code content is tested with other system in our
  scenario. Throwing it away hence helps to harden similarity score (ratio of sentences)
  and shorten analysis time,
   
- **Efficient candidate retrieval:** 
  - HNSW-based sentence search limits the candidates considered
    during sentence alignment, avoiding exhaustive comparison of every sentence pair. 
    In our setup it allowed reduction of considered sentences set by 80%, preserving high 
    recall.


The annotated benchmark contained 30 documents with 33 manually annotated comparisons. The larger
981-document experiment measured corpus-scale processing performance, these are separate
evaluations.


## Approach

This section extends previous overview with additional notes on preprocessing and comparison,
for formal specification see full text of the work.

### Preprocessing
Preprocessing pipeline can be summarized by pseudocode below:
```
Preprocess(text):
    tokens ← Tokenize(text)
    lines ← SplitIntoLines(tokens)
    classified ← ClassifyCode(lines)
    paragraphs ← GroupNaturalLanguageLines(classified)
    sentences ← SplitAtPunctuation(paragraphs)

    for each sentence:
        if tokenCount(sentence) > 15:
            clauses ← SplitIntoClauses(sentence)
            sentence ← MergeToMinimumLength(clauses, 5)

    for each sentence:
        tokens ← MorphologicalAnalysis(sentence)
        tokens ← PreserveWords(tokens)
        tokens ← RemoveShortWords(tokens)
        tokens ← RemoveStopwords(tokens)

        if tokenCount(tokens) < 2:
            Drop(sentence)

        tokens ← CorrectSpelling(tokens)
        tokens ← MorphologicalAnalysis(tokens)

    RemoveTemplateText(text, TemplateText)
```

Of notable:
  
- code classification
- sentence length balancing by splitting based on clause and merging
- spell checking
- template text removal (e.g task definition)

### Sentence comparison

Document comparison follows three stages:

1. **Seed:**
   - Candidate sentence matches are identified using bidirectional word alignment
     - Best matching word identified via cosine similarity over word2vec embeddings
   - Each word impact over sentence similarity score is derived from word's cosine similarity weighted by their 
     - POS coefficient: e.g. matched adjectives are more important,
     - corpus IDF: e.g. word "process" (OS term), is of none importance when matched, in context of OS university course assignment.

2. **Extend:** 
   - Nearby matches are recursively combined into larger matching fragments, 
   allowing for gaps and inserted or omitted sentences.
3. **Filter:** 
   - Short and overlapping fragments are removed to produce the final matches.

To avoid exhaustive sentence to sentence comparison, sentences are first represented as weighted sums
of their word embeddings, using the same POS $\times$ IDF weights. A **JVector HNSW index** 
retrieves candidate sentences for detailed comparison. In the evaluated configuration, 
the retrieval limit was set to 20% of the indexed sentence set.

For reference-corpus searches, **Apache Lucene** first retrieves potentially relevant documents. 
Their sentences are then indexed and compared using the same approach.

### Implementation constraints

The application was designed for a shared VM with limited available memory and a 
900-second execution limit for unprivileged processes. 
Memory profiling with **Java Mission Control and Java Flight Recorder** helped identify 
expensive object retention and duplicated text representations in the NLP pipeline.

To reduce peak memory usage, the implementation minimizes redundant strings, loads documents 
incrementally, and flushes comparison results to disk as soon as they are produced, rather than
keeping the entire comparison workload in memory.

The overall processing flow is illustrated below.

![Document processing activity diagram](docs/pipeline.svg)

## User interface

The Angular-based interface allows users to inspect document
comparisons, navigate between detected matching fragments, examine
document metadata, and explore similarities across submissions using
a graph and heatmap.

For this part of the project see [this](https://github.com/TarasHamkalo/plagdet-ui) repository.

## Running application
- add help output here, and jar to releases
- can contact me, to get ann dataset
- gradle version, java version

### Models installation

To download models run

```
pip install gensim
./scripts/download_models.sh
```

Default installation point in `models-tmp`.
After that copy:

- slovak-morfflex-pdt-170914-pos_only.tagger
- sk.poslemma.prim-10.0-public-saneword2vec.bin

Into resources root e.g. `plagdet-cli/resources`

