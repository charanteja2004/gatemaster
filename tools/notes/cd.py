# -*- coding: utf-8 -*-
"""Compiler Design revision notes."""

NOTES = {
    "cd_into_cd": {
        "title": "Introduction to Compiler Design",
        "blocks": [
            ("def", "A compiler translates a source program into an equivalent target program, reporting errors it finds along the way."),
            ("table", [
                ["Tool", "Does"],
                ["Compiler", "source to target, ahead of execution"],
                ["Interpreter", "executes the source directly"],
                ["Assembler", "assembly to machine code"],
                ["Linker", "joins object files and resolves references"],
                ["Loader", "places the image in memory to run"],
            ]),
            ("key", "The front end depends on the source language, the back end on the target machine. Splitting them at the intermediate representation is what lets n languages and m machines share n + m parts instead of needing n × m compilers."),
        ],
    },

    "cd_phases": {
        "title": "Phases of a Compiler",
        "blocks": [
            ("steps", [
                "Lexical analysis: characters to tokens.",
                "Syntax analysis: tokens to a parse tree.",
                "Semantic analysis: type and scope checking.",
                "Intermediate code generation.",
                "Code optimisation.",
                "Code generation: target instructions and registers.",
            ]),
            ("p", "The symbol table and the error handler are not phases; every phase talks to both."),
            ("table", [
                ["Error", "Caught by"],
                ["Illegal character, unterminated string", "lexical"],
                ["Missing semicolon, unbalanced brackets", "syntax"],
                ["Type mismatch, undeclared variable", "semantic"],
                ["Division by zero at run time", "no phase — it is not a compile-time error"],
            ]),
            ("key", "Which phase catches which error is the most reliably examined fact in this subject. int x = \"hello\" is lexically and syntactically fine, and fails only in semantic analysis."),
        ],
    },

    "cd_lexical": {
        "title": "Lexical Analysis",
        "blocks": [
            ("p", "The scanner groups characters into tokens, discards whitespace and comments, and enters identifiers into the symbol table."),
            ("table", [
                ["Term", "Means"],
                ["Token", "the category, such as identifier or operator"],
                ["Lexeme", "the actual characters matched"],
                ["Pattern", "the rule describing what matches"],
            ]),
            ("example", "In sum = a + 10, the tokens are id, assign, id, plus, number — five tokens, five lexemes, and only three patterns."),
            ("key", "Tokens form regular languages, so a finite automaton recognises them in linear time with no stack. That is precisely why lexing is separate from parsing."),
            ("note", "The longest-match rule resolves ambiguity: given >=, the scanner returns one token, not > followed by =. Keywords beat identifiers on ties."),
        ],
    },

    "cd_syntax": {
        "title": "Syntax Analysis",
        "blocks": [
            ("p", "The parser checks that the token stream fits the grammar and builds a parse tree recording the derivation."),
            ("def", "A context-free grammar is (V, T, P, S). Its productions have a single variable on the left, which is exactly what a stack can handle."),
            ("bullets", [
                "A parse tree shows every production applied; a syntax tree keeps only the operators and operands.",
                "A leftmost derivation always expands the leftmost variable; a rightmost derivation the rightmost.",
                "Top-down parsers trace a leftmost derivation; bottom-up parsers trace a rightmost one in reverse.",
            ]),
            ("key", "Nesting is why a regular grammar is not enough. Balanced brackets are the standard proof that the language of well-formed programs is not regular."),
        ],
    },

    "cd_parsing": {
        "title": "Parsing",
        "blocks": [
            ("table", [
                ["", "Top-down", "Bottom-up"],
                ["Builds from", "the root", "the leaves"],
                ["Derivation traced", "leftmost", "rightmost, reversed"],
                ["Cannot handle", "left recursion", "—"],
                ["Examples", "recursive descent, LL(1)", "SLR, LALR, CLR"],
            ]),
            ("def", "A handle is a substring matching a production's right side whose reduction is a step in the reverse rightmost derivation. Bottom-up parsing is repeated handle pruning."),
            ("bullets", [
                "Shift: push the next token.",
                "Reduce: replace a handle on the stack by the production's left side.",
                "Accept, or error.",
            ]),
            ("key", "LR parsers are strictly more powerful than LL parsers, because deciding after seeing the whole right side is easier than deciding before seeing any of it."),
        ],
    },

    "cd_ambigious": {
        "title": "Ambiguous Grammars",
        "blocks": [
            ("def", "A grammar is ambiguous if some string has two distinct parse trees, equivalently two distinct leftmost derivations."),
            ("example", "E → E + E | E * E | id makes id + id * id parse two ways. Adding precedence levels — E → E + T, T → T * F, F → id — removes the ambiguity and encodes the precedence."),
            ("key", "The dangling else is the other standard case: matching the else to the nearest unmatched if is a rule imposed on top of an ambiguous grammar."),
            ("warn", "Ambiguity of an arbitrary context-free grammar is undecidable, so no tool can flag it in general. Parser generators report conflicts, which are a symptom rather than a test."),
            ("note", "Ambiguity belongs to the grammar. Some languages are inherently ambiguous, meaning no unambiguous grammar for them exists at all."),
        ],
    },

    "cd_topdown": {
        "title": "Top-Down Parsers",
        "blocks": [
            ("p", "Start at the start symbol and expand, predicting which production to use from the lookahead token."),
            ("bullets", [
                "Recursive descent: one function per non-terminal, possibly with backtracking.",
                "Predictive (LL(1)): a parsing table indexed by non-terminal and lookahead, no backtracking.",
            ]),
            ("h", "Preparing a grammar"),
            ("formula", "Left recursion:  A → Aα | β  becomes  A → βA',  A' → αA' | ε"),
            ("formula", "Left factoring:  A → αβ | αγ  becomes  A → αA',  A' → β | γ"),
            ("h", "Is it LL(1)?"),
            ("bullets", [
                "For A → α | β, FIRST(α) ∩ FIRST(β) must be empty.",
                "If β can derive ε, FIRST(α) ∩ FOLLOW(A) must be empty.",
            ]),
            ("warn", "Left recursion makes a recursive descent parser call itself with no input consumed — an infinite loop, not a wrong answer. Bottom-up parsers handle it without complaint."),
        ],
    },

    "cd_slr": {
        "title": "SLR Parser",
        "blocks": [
            ("p", "Build the LR(0) item sets, then decide reductions using the FOLLOW set of the left-hand side."),
            ("steps", [
                "Augment the grammar with S' → S.",
                "Compute the LR(0) canonical collection with closure and goto.",
                "For item A → α· with A ≠ S', put reduce in every column of FOLLOW(A).",
                "Shift entries come from goto on terminals.",
            ]),
            ("key", "FOLLOW is a global set, computed without regard to the state the parser is in. That coarseness is the whole weakness of SLR: it may place a reduce where the context forbids it, creating a conflict a finer lookahead would avoid."),
            ("note", "Every SLR(1) grammar is LALR(1) and CLR(1). The reverse fails, which is what the classic three-way comparison questions turn on."),
        ],
    },

    "cd_clr": {
        "title": "CLR Parser",
        "blocks": [
            ("p", "Canonical LR carries a lookahead symbol inside every item, so reductions are decided per state rather than globally."),
            ("table", [
                ["", "SLR(1)", "CLR(1)", "LALR(1)"],
                ["Lookahead", "FOLLOW set", "per-item", "merged per-item"],
                ["States", "fewest", "most", "same as SLR"],
                ["Power", "least", "most", "in between"],
            ]),
            ("key", "CLR is the most powerful of the three and the least practical: the state count can be several times larger for a real language grammar."),
        ],
    },

    "cd_lalr": {
        "title": "LALR Parser",
        "blocks": [
            ("p", "Take the CLR automaton and merge states with identical cores, unioning their lookaheads."),
            ("key", "Merging can create reduce-reduce conflicts but never shift-reduce conflicts, because shifts depend only on the core, which merging leaves untouched."),
            ("note", "LALR gives nearly CLR's power with SLR's table size, which is why YACC and Bison generate LALR(1) parsers by default."),
            ("warn", "An LALR parser may perform some extra reductions before announcing an error, though it never shifts a symbol a CLR parser would reject."),
        ],
    },

    "cd_sematic": {
        "title": "Syntax-Directed Translation",
        "blocks": [
            ("def", "A syntax-directed definition attaches attributes to grammar symbols and rules for computing them to productions."),
            ("table", [
                ["Attribute", "Computed from", "Flows"],
                ["Synthesised", "the children", "upward"],
                ["Inherited", "the parent or siblings", "downward and sideways"],
            ]),
            ("example", "In E → E1 + T, the rule E.val = E1.val + T.val is synthesised. In D → T L, the rule L.type = T.type is inherited."),
            ("key", "Synthesised attributes can be evaluated during a bottom-up parse, which is why S-attributed definitions fit LR parsing exactly."),
        ],
    },

    "cd_s_l_attribute": {
        "title": "S-attributed and L-attributed Definitions",
        "blocks": [
            ("bullets", [
                "S-attributed: only synthesised attributes. Evaluable in one bottom-up pass.",
                "L-attributed: synthesised attributes, plus inherited ones that depend only on the parent and on siblings to the left. Evaluable in one left-to-right depth-first pass.",
            ]),
            ("key", "Every S-attributed definition is L-attributed; the reverse is false. L-attributed is exactly the class a one-pass compiler can evaluate while parsing."),
            ("warn", "An inherited attribute depending on a sibling to the right breaks the single pass, because that sibling has not been parsed yet."),
        ],
    },

    "cd_symbolic_table": {
        "title": "Symbol Table",
        "blocks": [
            ("def", "The symbol table records every identifier with its type, scope, storage class and memory offset."),
            ("table", [
                ["Structure", "Lookup", "Note"],
                ["Linear list", "O(n)", "simple, too slow"],
                ["Binary search tree", "O(log n)", "ordered"],
                ["Hash table", "O(1) average", "what compilers use"],
            ]),
            ("key", "Nested scopes are handled by a stack of tables, or by chaining entries per name. Entering a block pushes a table and leaving it pops, which gives correct shadowing for free."),
            ("note", "The table is built during lexical and syntax analysis and consulted heavily during semantic analysis and code generation, which is why lookup speed matters."),
        ],
    },

    "cd_error": {
        "title": "Error Detection and Recovery",
        "blocks": [
            ("p", "A compiler should report the first error accurately and keep going, so one run finds many errors rather than one."),
            ("table", [
                ["Strategy", "Does"],
                ["Panic mode", "skip tokens until a synchronising token such as ; or }"],
                ["Phrase level", "perform a local correction and continue"],
                ["Error productions", "add rules for common mistakes to report them precisely"],
                ["Global correction", "find the smallest edit making the input legal"],
            ]),
            ("key", "Panic mode is the standard because it cannot loop and is trivial to implement. Global correction is optimal and too expensive to use."),
            ("warn", "Cascading errors are the real danger: one missing brace can produce dozens of meaningless messages after it, which is what synchronising tokens exist to stop."),
        ],
    },

    "cd_intermediate_code_generator": {
        "title": "Intermediate Code Generation",
        "blocks": [
            ("p", "An intermediate representation sits between the source and the target: independent of both, easy to optimise, easy to translate."),
            ("bullets", [
                "Syntax trees and DAGs: structural, good for local optimisation.",
                "Postfix notation: compact, operand order fixed.",
                "Three-address code: the workhorse, one operator per instruction.",
            ]),
            ("key", "A DAG differs from a syntax tree by sharing identical subexpressions, which is how common subexpression elimination is discovered in the first place."),
        ],
    },

    "cd_threeaddress": {
        "title": "Three-Address Code",
        "blocks": [
            ("p", "Each instruction has at most one operator and at most three operands, so complex expressions need temporaries."),
            ("code", ("text", "a = b * c + d * e\n\nt1 = b * c\nt2 = d * e\na  = t1 + t2")),
            ("table", [
                ["Representation", "Fields", "Note"],
                ["Quadruple", "op, arg1, arg2, result", "result named explicitly"],
                ["Triple", "op, arg1, arg2", "referred to by position"],
                ["Indirect triple", "a list of pointers to triples", "reordering is cheap"],
            ]),
            ("key", "Triples save space but make code motion painful, because moving an instruction changes the position every reference used. Indirect triples exist precisely to fix that."),
        ],
    },

    "cd_basicblock": {
        "title": "Basic Blocks and Flow Graphs",
        "blocks": [
            ("def", "A basic block is a maximal run of instructions entered only at the first and left only at the last."),
            ("h", "Finding the leaders"),
            ("steps", [
                "The first instruction is a leader.",
                "Any target of a jump is a leader.",
                "Any instruction immediately after a jump is a leader.",
                "Each block runs from a leader to just before the next.",
            ]),
            ("p", "A flow graph has basic blocks as nodes and possible control transfers as edges."),
            ("key", "Within a block every instruction executes exactly when the first does, which is what makes local optimisation safe with no analysis of the rest of the program."),
            ("note", "A loop in the flow graph is found by a back edge to a dominator — the entry point of that loop."),
        ],
    },

    "cd_codeoptimization": {
        "title": "Code Optimisation",
        "blocks": [
            ("table", [
                ["Optimisation", "Does"],
                ["Constant folding", "evaluates constant expressions at compile time"],
                ["Constant propagation", "replaces a variable by its known constant"],
                ["Common subexpression elimination", "computes a repeated expression once"],
                ["Dead code elimination", "removes computations never used"],
                ["Copy propagation", "replaces x by y after x = y"],
                ["Strength reduction", "swaps an expensive operator for a cheaper one"],
            ]),
            ("bullets", [
                "Local: inside one basic block.",
                "Global: across blocks, needing data flow analysis.",
                "Interprocedural: across functions.",
            ]),
            ("key", "Machine-independent optimisations work on the intermediate code; register allocation and instruction scheduling are machine-dependent and belong to the back end."),
            ("warn", "Every optimisation must preserve meaning. Reordering floating point arithmetic changes results, which is why compilers will not do it unless told to."),
        ],
    },

    "cd_loop": {
        "title": "Loop Optimisation",
        "blocks": [
            ("p", "Loops carry most of the run time, so they repay optimisation more than anything else."),
            ("table", [
                ["Technique", "Does"],
                ["Code motion", "hoists loop-invariant computations to a pre-header"],
                ["Strength reduction", "turns i * w into a running sum"],
                ["Induction variable elimination", "removes a variable derivable from another"],
                ["Loop unrolling", "repeats the body to cut test and branch overhead"],
                ["Loop fusion", "merges two loops over the same range"],
            ]),
            ("example", "Array addressing a[i] with element width 4 computes i * 4 each iteration. Strength reduction keeps a pointer and adds 4, replacing a multiply with an add."),
            ("key", "A computation is loop-invariant only if nothing it depends on is assigned inside the loop. Hoisting anything else changes the program."),
        ],
    },

    "cd_peephole": {
        "title": "Peephole Optimisation",
        "blocks": [
            ("p", "Slide a small window over the generated code and replace recognised patterns with better ones."),
            ("bullets", [
                "Remove a load immediately following a store to the same location.",
                "Delete a jump to the very next instruction.",
                "Simplify algebra: x = x + 0, x = x * 1.",
                "Replace multiplication by a power of two with a shift.",
                "Collapse a jump to a jump into a single jump.",
            ]),
            ("key", "The window is narrow, which makes it cheap and local. Repeated passes matter because one replacement often exposes another."),
            ("note", "Usually applied to target code, where code generation has just introduced the redundancies, though it works on intermediate code too."),
        ],
    },
}
