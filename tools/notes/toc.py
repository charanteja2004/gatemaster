# -*- coding: utf-8 -*-
"""Theory of Computation revision notes."""

NOTES = {
    "toc_intro_toc": {
        "title": "Introduction to Theory of Computation",
        "blocks": [
            ("p", "Theory of Computation asks two questions about problems rather than about programs: what can be computed at all, and what can be computed efficiently. GATE draws almost every question from the first."),
            ("def", "A language is a set of strings over a finite alphabet. A machine accepts a language if it halts in an accepting state on exactly the strings in that set."),
            ("h", "The three layers"),
            ("bullets", [
                "Automata: the machine models, from finite automata up to Turing machines.",
                "Formal languages: the grammars that generate what those machines accept.",
                "Computability and complexity: what no machine can decide, and what no machine can decide quickly.",
            ]),
            ("note", "Every model is defined by what memory it has. A finite automaton has none beyond its state, a pushdown automaton has a stack, a Turing machine has an unbounded tape. That single difference decides everything else."),
            ("key", "The hierarchy is strict: every regular language is context-free, every context-free language is recursive, and each containment misses something. aⁿbⁿ is context-free but not regular; aⁿbⁿcⁿ is not even context-free."),
        ],
    },

    "toc_finiteautomata": {
        "title": "Introduction to Finite Automata",
        "blocks": [
            ("p", "A finite automaton is the weakest useful machine: a fixed set of states, one input symbol at a time, and no memory of what it has read beyond the state it is in."),
            ("def", "A DFA is a 5-tuple (Q, Σ, δ, q₀, F): states, alphabet, transition function δ: Q × Σ → Q, a start state, and a set of accepting states."),
            ("h", "DFA against NFA"),
            ("table", [
                ["", "DFA", "NFA"],
                ["Transitions", "exactly one per symbol", "zero, one or many"],
                ["Epsilon moves", "no", "yes"],
                ["Power", "regular languages", "regular languages"],
                ["States for the same language", "can be exponentially more", "often far fewer"],
            ]),
            ("key", "An NFA is no more powerful than a DFA. Subset construction converts any NFA with n states into a DFA with at most 2ⁿ states, and that bound is tight for some languages."),
            ("h", "The pumping lemma"),
            ("p", "The standard tool for proving a language is not regular. If L is regular there is a length p such that every string s in L with |s| ≥ p splits as s = xyz with |xy| ≤ p, |y| ≥ 1, and xyⁱz in L for every i ≥ 0."),
            ("example", "For L = {aⁿbⁿ}, take s = aᵖbᵖ. Any y inside the first p characters is all a's, so pumping changes the number of a's but not the b's and leaves the language. Hence L is not regular."),
            ("warn", "The pumping lemma is a one-way test. Failing it proves irregularity; passing it proves nothing, because some non-regular languages satisfy it."),
        ],
    },

    "toc_mealymoore": {
        "title": "Mealy and Moore Machines",
        "blocks": [
            ("p", "Both are finite automata that emit output as well as consume input. They differ only in where the output is attached."),
            ("table", [
                ["", "Moore", "Mealy"],
                ["Output depends on", "the state alone", "the state and the input symbol"],
                ["Output written on", "states", "transitions"],
                ["Output length for input of length n", "n + 1", "n"],
                ["Reacts to an input", "one clock later", "in the same step"],
            ]),
            ("key", "The extra symbol in a Moore machine comes from the start state, which emits before any input is read. That off-by-one is the most commonly examined difference."),
            ("note", "The two are equally powerful. A Moore machine with n states converts to a Mealy machine with n states; a Mealy machine with n states converts to a Moore machine with at most n × |output alphabet| states, because a state must be split once per distinct output it produced."),
            ("example", "A machine that outputs 1 whenever it has just read the pattern 11 is naturally Mealy: the output belongs to the transition that completes the pattern, not to a state."),
        ],
    },

    "toc_grammer": {
        "title": "Grammars and the Languages They Generate",
        "blocks": [
            ("def", "A grammar is a 4-tuple (V, T, P, S): variables, terminals, productions, and a start variable. It generates a string when repeated substitution from S reaches a string of terminals only."),
            ("p", "Machines accept languages; grammars generate them. For every class in the hierarchy the two views coincide, which is why a proof can move freely between them."),
            ("table", [
                ["Grammar", "Machine", "Production shape"],
                ["Regular", "Finite automaton", "A → aB or A → a"],
                ["Context-free", "Pushdown automaton", "A → α"],
                ["Context-sensitive", "Linear bounded automaton", "αAβ → αγβ, |γ| ≥ 1"],
                ["Unrestricted", "Turing machine", "α → β"],
            ]),
            ("key", "The left side of a context-free production is a single variable. That single restriction is what a stack is exactly powerful enough to handle."),
            ("note", "A grammar is ambiguous when some string has two distinct parse trees. Ambiguity is a property of the grammar, not the language: many ambiguous grammars have unambiguous equivalents, though inherently ambiguous languages exist."),
        ],
    },

    "toc_chomsky_hirarky": {
        "title": "Chomsky Hierarchy",
        "blocks": [
            ("p", "Four nested classes of language, each defined by how much a machine may remember and how freely a grammar may rewrite."),
            ("table", [
                ["Type", "Language class", "Recognised by", "Closed under intersection?"],
                ["0", "Recursively enumerable", "Turing machine", "yes"],
                ["1", "Context-sensitive", "Linear bounded automaton", "yes"],
                ["2", "Context-free", "Pushdown automaton", "no"],
                ["3", "Regular", "Finite automaton", "yes"],
            ]),
            ("key", "Type 3 ⊂ Type 2 ⊂ Type 1 ⊂ Type 0, and every containment is strict."),
            ("example", "aⁿbⁿ separates regular from context-free. aⁿbⁿcⁿ separates context-free from context-sensitive. The halting problem's language separates recursive from recursively enumerable."),
            ("warn", "Context-free languages are not closed under intersection or complement, unlike every other class here. The intersection of {aⁿbⁿcᵐ} and {aᵐbⁿcⁿ} is {aⁿbⁿcⁿ}, which is not context-free."),
        ],
    },

    "toc_application_of_autometa": {
        "title": "Where Each Automaton Is Used",
        "blocks": [
            ("p", "The hierarchy is not only theory: each level names the tool a compiler or a system actually reaches for."),
            ("table", [
                ["Model", "Used for"],
                ["Finite automaton", "Lexical analysis, text search, protocol and UI state machines"],
                ["Pushdown automaton", "Parsing nested syntax: brackets, blocks, expressions"],
                ["Linear bounded automaton", "Context-sensitive checks such as declare-before-use"],
                ["Turing machine", "The reference model for what is computable at all"],
            ]),
            ("key", "The split between lexing and parsing in every compiler is exactly the split between regular and context-free. Tokens have no nesting, so a finite automaton suffices and runs in linear time with no stack."),
            ("note", "Regular expressions in a text editor are the same machine in another notation. Some library flavours add backreferences, which take them beyond regular and can cost exponential time."),
        ],
    },

    "toc_computable": {
        "title": "Computable and Non-computable Problems",
        "blocks": [
            ("def", "A language is recursive (decidable) if some Turing machine halts on every input and answers correctly. It is recursively enumerable if a machine halts and accepts on the strings in the language, but may loop forever on the ones outside it."),
            ("key", "Recursive ⊂ recursively enumerable, strictly. A language is recursive exactly when both it and its complement are recursively enumerable."),
            ("h", "The halting problem"),
            ("p", "Given a program and an input, decide whether it halts. No algorithm does this for every case. The proof assumes a decider H exists, builds a program that consults H about itself and does the opposite, and derives a contradiction."),
            ("note", "The halting language is recursively enumerable: simulate, and accept if the simulation stops. What is impossible is recognising the machines that never stop."),
            ("h", "Rice's theorem"),
            ("p", "Every non-trivial property of the language a Turing machine recognises is undecidable. Non-trivial means some machines have the property and some do not."),
            ("example", "\"Does this program ever print anything?\" and \"Do these two programs compute the same function?\" are both undecidable by Rice's theorem. \"Does this program have more than 50 lines?\" is decidable, because it is a property of the code rather than of the language recognised."),
            ("warn", "Undecidable does not mean hard. It means no algorithm is correct on every input, however long it is allowed to run. NP-hard problems, by contrast, are decidable and merely expensive."),
        ],
    },
}
