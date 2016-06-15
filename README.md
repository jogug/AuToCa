AuToCa
======

Given an unknown programming language, AuToCa attempts to automatically recognize which are the tokens of the language

Abstract

Developers need software models to make decisions while developing
software systems. Static software models are commonly imported by parsing
source code. But building a custom parser is a difficult task in most
programming languages. To build such a custom parser the grammar of the
programming language is needed. If the grammar is not available, which is
the case for many languages and notably dialects, it has to be inferred from
the source code. Automatically finding the keywords of those languages can
help the process of inferring a grammar because many keywords identify
beginnings and endings of the basic building blocks of programs.
We tested four heuristics of finding keywords in source code of unknown
languages: i) the most common words are keywords; ii) words that occur
in most of the files are keywords; iii) words at the first position of the line
before an indent are keywords; and iv) words at the beginning of a line are
keywords.
With our third method we achieved the best results. It found 26 of the
50 Java keywords, 10 out of 17 of the Shell keywords and 9 out of 20 of the
Haskell keywords. Our data suggests that the more source code is available
the more precise the results get. Adding more source code produced the
most improvements when using the first or second method.

http://scg.unibe.ch/archive/projects/Gugg15a.pdf
