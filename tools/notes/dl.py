# -*- coding: utf-8 -*-
"""Digital Logic revision notes."""

NOTES = {
    "dl_numbersystem": {
        "title": "Number Systems and Base Conversion",
        "blocks": [
            ("table", [
                ["Base", "Digits", "Use"],
                ["2", "0, 1", "everything inside the machine"],
                ["8", "0–7", "3 bits per digit"],
                ["10", "0–9", "human"],
                ["16", "0–9, A–F", "4 bits per digit"],
            ]),
            ("steps", [
                "Integer to another base: divide repeatedly, read remainders bottom-up.",
                "Fraction to another base: multiply repeatedly, read integer parts top-down.",
                "Binary to octal or hex: group bits in 3s or 4s from the binary point.",
            ]),
            ("example", "(1011.11)₂ = 8 + 2 + 1 + 0.5 + 0.25 = 11.75.  (2C)₁₆ = 0010 1100 = (54)₈."),
            ("warn", "Group from the binary point outwards, not from the left. Grouping 101100 from the wrong end gives the wrong octal digits."),
        ],
    },

    "dl_12": {
        "title": "1's and 2's Complement",
        "blocks": [
            ("bullets", [
                "1's complement: invert every bit.",
                "2's complement: invert every bit and add 1.",
                "Shortcut: copy from the right up to and including the first 1, invert the rest.",
            ]),
            ("table", [
                ["", "1's complement", "2's complement"],
                ["Range in n bits", "−(2ⁿ⁻¹−1) … 2ⁿ⁻¹−1", "−2ⁿ⁻¹ … 2ⁿ⁻¹−1"],
                ["Zero", "two (+0 and −0)", "one"],
                ["End-around carry", "needed", "discarded"],
            ]),
            ("example", "8-bit: 00010110 is 22; its 2's complement is 11101010 = −22.  11111011 is −5."),
            ("key", "2's complement wins because addition and subtraction use the same adder and there is a single zero. The asymmetric range — one more negative than positive — follows from that."),
            ("note", "Signed overflow is detected when the carry into the sign bit differs from the carry out of it."),
        ],
    },

    "dl_greycode": {
        "title": "Gray Code",
        "blocks": [
            ("def", "Gray code is an ordering of binary values in which consecutive values differ in exactly one bit."),
            ("table", [
                ["Decimal", "Binary", "Gray"],
                ["0", "000", "000"],
                ["1", "001", "001"],
                ["2", "010", "011"],
                ["3", "011", "010"],
                ["4", "100", "110"],
            ]),
            ("key", "One bit changing at a time means a reading taken mid-transition is at worst off by one position, which is why shaft encoders use it. It is unweighted, so you cannot read its value positionally."),
            ("note", "The rows and columns of a K-map are labelled in Gray code, which is exactly why adjacent cells differ in one variable and can be grouped."),
        ],
    },

    "dl_binarytogray": {
        "title": "Code Converters: Binary and Gray",
        "blocks": [
            ("formula", "Binary to Gray:  g(n) = b(n),  g(i) = b(i+1) ⊕ b(i)"),
            ("formula", "Gray to binary:  b(n) = g(n),  b(i) = b(i+1) ⊕ g(i)"),
            ("example", "Binary 1010 → Gray 1111.  Gray 1110 → binary 1011."),
            ("key", "Binary to Gray is a parallel operation: every output bit depends on two adjacent input bits. Gray to binary is sequential, because each output feeds the next XOR — a chain, not a fan-out."),
        ],
    },

    "dl_floatpoint": {
        "title": "Floating Point Representation",
        "blocks": [
            ("formula", "value = (−1)^sign × 1.mantissa × 2^(exponent − bias)"),
            ("table", [
                ["", "Single (32-bit)", "Double (64-bit)"],
                ["Sign", "1", "1"],
                ["Exponent", "8", "11"],
                ["Mantissa stored", "23", "52"],
                ["Bias", "127", "1023"],
            ]),
            ("key", "The leading 1 of a normalised number is implicit and not stored, so 23 stored bits give 24 bits of precision. Exponent fields of all 0s and all 1s are reserved for zero, denormals, infinity and NaN."),
            ("warn", "Floating point addition is not associative. (a + b) + c can differ from a + (b + c) because each rounding loses different bits."),
        ],
    },

    "dl_boolean": {
        "title": "Boolean Algebra",
        "blocks": [
            ("table", [
                ["Law", "Form"],
                ["Identity", "A + 0 = A,  A · 1 = A"],
                ["Null", "A + 1 = 1,  A · 0 = 0"],
                ["Idempotent", "A + A = A,  A · A = A"],
                ["Complement", "A + A' = 1,  A · A' = 0"],
                ["Absorption", "A + AB = A,  A(A + B) = A"],
                ["De Morgan", "(AB)' = A' + B',  (A + B)' = A'B'"],
            ]),
            ("key", "A + A'B = A + B is the identity most often missed. It is not absorption: the complemented literal drops out, but B survives."),
            ("note", "Every law comes in a dual pair — swap AND with OR and 0 with 1. Proving one form proves the other."),
        ],
    },

    "dl_rep_boolean": {
        "title": "Representing Boolean Functions",
        "blocks": [
            ("bullets", [
                "Truth table: exhaustive, 2ⁿ rows, unambiguous.",
                "Sum of products: an OR of AND terms, one per row that outputs 1.",
                "Product of sums: an AND of OR terms, one per row that outputs 0.",
                "K-map: the truth table drawn so adjacent cells differ in one variable.",
            ]),
            ("formula", "Number of distinct functions of n variables = 2^(2ⁿ)"),
            ("example", "For 2 variables there are 16 distinct functions; for 3 there are 256."),
            ("key", "SOP and POS describe the same function. Which is smaller depends on whether the function has fewer 1s or fewer 0s."),
        ],
    },

    "dl_min_boolean": {
        "title": "Minimising Boolean Functions",
        "blocks": [
            ("p", "Minimisation means the fewest terms and the fewest literals, which translates directly into gates and gate inputs."),
            ("bullets", [
                "Algebraic: apply the laws until nothing more cancels. No guarantee of a minimum.",
                "K-map: reliable up to about five variables, by grouping adjacent 1s.",
                "Quine-McCluskey: tabular, systematic, works for any n and is programmable.",
            ]),
            ("example", "AB + AB' = A(B + B') = A.  (A + B)(A + B') = A, its dual."),
            ("key", "A group of 2^k cells in a K-map removes k literals. That is why groups must be powers of two and as large as possible."),
        ],
    },

    "dl_conical": {
        "title": "Canonical and Standard Forms",
        "blocks": [
            ("def", "In canonical form every term contains every variable: minterms for SOP, maxterms for POS. Standard form drops that requirement."),
            ("formula", "f = Σm(…) = ΠM(…), where the maxterm indices are exactly the minterm indices that are absent"),
            ("example", "For three variables, f = Σm(0,1,2,3) = ΠM(4,5,6,7), and both simplify to A'."),
            ("key", "Minterm m(i) is 1 on exactly one row; maxterm M(i) is 0 on exactly one row. Complementing a function swaps the two lists."),
        ],
    },

    "dl_kmap": {
        "title": "Karnaugh Maps",
        "blocks": [
            ("p", "A K-map lays the truth table out in Gray-code order so that logically adjacent minterms are physically adjacent and can be grouped."),
            ("steps", [
                "Fill the map from the truth table.",
                "Group 1s in blocks of 1, 2, 4, 8… as large as possible, wrapping around edges.",
                "Every 1 must be covered; groups may overlap.",
                "Read each group as the product of the variables that stay constant in it.",
            ]),
            ("example", "F(A,B,C,D) = Σm(0,2,8,10): A and C vary while B = 0 and D = 0, so the group of four reduces to B'D'."),
            ("key", "Don't-care cells may be treated as 1 or 0, whichever makes the groups larger. Using them well is usually the difference between the intended answer and a longer one."),
            ("note", "A prime implicant is a group that cannot be enlarged; an essential prime implicant is one covering a 1 that no other group covers. Essential ones must appear in the answer."),
        ],
    },

    "dl_logicgates": {
        "title": "Logic Gates",
        "blocks": [
            ("table", [
                ["Gate", "Output is 1 when"],
                ["AND", "all inputs are 1"],
                ["OR", "any input is 1"],
                ["NOT", "the input is 0"],
                ["NAND", "not all inputs are 1"],
                ["NOR", "all inputs are 0"],
                ["XOR", "an odd number of inputs are 1"],
                ["XNOR", "an even number of inputs are 1"],
            ]),
            ("key", "NAND and NOR are universal: each alone can build NOT, AND and OR, and therefore any function. AND and OR together cannot, because neither can complement."),
            ("bullets", [
                "NOT from NAND: tie the inputs together.",
                "AND from NAND: a NAND followed by a NAND used as NOT.",
                "OR from NAND: invert both inputs, then NAND.",
            ]),
            ("note", "XOR is the parity and inequality gate: it detects a difference on two inputs, and odd parity on more."),
        ],
    },

    "dl_half": {
        "title": "Half Adder",
        "blocks": [
            ("formula", "Sum = A ⊕ B,  Carry = A · B"),
            ("p", "Adds two single bits and produces a sum and a carry. It cannot accept a carry from a previous stage, which is exactly what makes it 'half'."),
            ("key", "Two half adders and one OR gate make a full adder. That construction is asked more often than either circuit on its own."),
        ],
    },

    "dl_full": {
        "title": "Full Adder",
        "blocks": [
            ("formula", "Sum = A ⊕ B ⊕ Cin"),
            ("formula", "Cout = AB + Cin(A ⊕ B)"),
            ("p", "Adds three bits — two operands and a carry-in — producing a sum and a carry-out."),
            ("key", "The carry expression splits into generate (AB) and propagate (A ⊕ B). Naming those two terms is what makes carry look-ahead possible."),
            ("note", "n full adders chained by their carries form a ripple carry adder, which is correct but slow: the last sum bit waits for a carry that has crossed every stage."),
        ],
    },

    "dl_carry": {
        "title": "Carry Look-Ahead Adder",
        "blocks": [
            ("p", "Compute all carries directly from the inputs instead of waiting for them to ripple."),
            ("formula", "G(i) = A(i)·B(i),  P(i) = A(i) ⊕ B(i)"),
            ("formula", "C(i+1) = G(i) + P(i)·C(i)"),
            ("key", "Expanding the recurrence gives every carry as a two-level function of the inputs, so addition takes constant gate delay rather than delay proportional to n."),
            ("warn", "The cost is fan-in and gate count, both growing quickly with width. Real adders use look-ahead in blocks of four and ripple between blocks."),
            ("table", [
                ["Adder", "Delay for n bits"],
                ["Ripple carry", "O(n)"],
                ["Carry look-ahead", "O(log n) in practice, O(1) for one block"],
            ]),
        ],
    },

    "dl_parallel": {
        "title": "Parallel Adder and Subtractor",
        "blocks": [
            ("p", "n full adders side by side add two n-bit numbers. The same circuit subtracts when the second operand is complemented."),
            ("key", "Feed B through XOR gates controlled by a mode line M, and tie M to the carry-in. M = 0 gives A + B; M = 1 gives A + B' + 1, which is A − B in 2's complement."),
            ("warn", "Overflow is not the carry-out. For signed operands, overflow is the XOR of the carry into and out of the most significant bit."),
        ],
    },

    "dl_encode": {
        "title": "Encoders and Decoders",
        "blocks": [
            ("table", [
                ["", "Decoder", "Encoder"],
                ["Inputs", "n", "2ⁿ"],
                ["Outputs", "2ⁿ", "n"],
                ["Asserts", "one output per input code", "the code of the active input"],
            ]),
            ("key", "A decoder generates every minterm of its inputs, so a decoder plus an OR gate implements any function directly from its Σm list."),
            ("warn", "A plain encoder is undefined when two inputs are active at once. A priority encoder resolves it by encoding the highest-numbered active input."),
            ("note", "An enable line turns a decoder into a demultiplexer: the same hardware, addressed by which view you take of the inputs."),
        ],
    },

    "dl_multiplex": {
        "title": "Multiplexers",
        "blocks": [
            ("def", "A multiplexer selects one of 2ⁿ data inputs onto a single output, chosen by n select lines."),
            ("formula", "2-to-1: Y = S'·I0 + S·I1"),
            ("key", "A 2ⁿ-to-1 multiplexer implements any function of n variables directly: wire the truth table's output column to the data inputs and the variables to the selects."),
            ("example", "With one extra inverter, a 2ⁿ⁻¹-to-1 multiplexer handles n variables: use n−1 as selects and feed 0, 1, the last variable or its complement to each data input."),
            ("note", "Building a 16-to-1 from 4-to-1 multiplexers takes five: four in the first stage, one to select among their outputs."),
        ],
    },

    "dl_demux": {
        "title": "Demultiplexers",
        "blocks": [
            ("def", "A demultiplexer routes one input to one of 2ⁿ outputs, chosen by n select lines. It is the inverse of a multiplexer."),
            ("key", "A demultiplexer is a decoder whose enable input carries the data. Textbooks list them separately; the silicon is the same."),
            ("note", "Multiplexer then demultiplexer over a shared line is time-division multiplexing — one wire carrying many logical channels."),
        ],
    },

    "dl_introsequntial": {
        "title": "Sequential Circuits",
        "blocks": [
            ("def", "A sequential circuit's output depends on the input and on stored state, unlike a combinational circuit which depends only on the present input."),
            ("table", [
                ["", "Combinational", "Sequential"],
                ["Memory", "none", "flip-flops"],
                ["Output depends on", "current inputs", "inputs and state"],
                ["Analysed with", "truth tables", "state tables and diagrams"],
            ]),
            ("bullets", [
                "Synchronous: state changes only on a clock edge. Predictable, and the norm.",
                "Asynchronous: state changes as inputs change. Faster, prone to races and hazards.",
            ]),
        ],
    },

    "dl_flipflop": {
        "title": "Flip-Flops",
        "blocks": [
            ("table", [
                ["Type", "Inputs", "Characteristic equation", "Note"],
                ["SR", "S, R", "Q⁺ = S + R'Q", "S = R = 1 forbidden"],
                ["D", "D", "Q⁺ = D", "no forbidden state"],
                ["JK", "J, K", "Q⁺ = JQ' + K'Q", "J = K = 1 toggles"],
                ["T", "T", "Q⁺ = T ⊕ Q", "toggles when T = 1"],
            ]),
            ("key", "JK removes the SR forbidden state by turning it into a toggle, which is what makes counters possible. D is what registers are built from."),
            ("h", "Latch against flip-flop"),
            ("note", "A latch is level-sensitive and transparent while enabled; a flip-flop is edge-triggered and samples at one instant. That distinction decides whether a circuit is racy."),
            ("warn", "Setup and hold times bound how close to the clock edge the input may change. Violating them gives metastability, not merely a wrong value."),
        ],
    },

    "dl_masterslave": {
        "title": "Master-Slave Flip-Flop",
        "blocks": [
            ("p", "Two latches in series on opposite clock phases: the master samples while the clock is high, the slave copies while it is low."),
            ("key", "Only one latch is ever transparent, so the output cannot feed back into the input within a clock period. That is what eliminates the race-around condition of a level-triggered JK."),
            ("note", "The effect is edge triggering built from level-sensitive parts, which is why the arrangement predates true edge-triggered designs."),
        ],
    },

    "dl_counters": {
        "title": "Counters",
        "blocks": [
            ("table", [
                ["", "Asynchronous (ripple)", "Synchronous"],
                ["Clocking", "each stage from the previous output", "all stages share the clock"],
                ["Delay", "accumulates along the chain", "one flip-flop delay"],
                ["Glitches", "yes, during propagation", "no"],
                ["Hardware", "minimal", "extra combinational logic"],
            ]),
            ("formula", "n flip-flops count 0…2ⁿ−1; a mod-m counter needs n = ⌈log₂ m⌉ flip-flops"),
            ("example", "A mod-10 counter needs 4 flip-flops and resets when it detects 1010, leaving six unused states."),
            ("key", "Ripple counters are cheap and slow; the accumulated delay caps the counting frequency and produces transient wrong values on the outputs."),
        ],
    },

    "dl_shiftregistors": {
        "title": "Shift Registers",
        "blocks": [
            ("table", [
                ["Type", "In", "Out"],
                ["SISO", "serial", "serial"],
                ["SIPO", "serial", "parallel"],
                ["PISO", "parallel", "serial"],
                ["PIPO", "parallel", "parallel"],
            ]),
            ("bullets", [
                "Serial to parallel conversion and back, as in UARTs.",
                "Delay lines: n stages delay data by n clocks.",
                "Ring counter: n flip-flops, n states, one-hot.",
                "Johnson counter: n flip-flops, 2n states, feedback inverted.",
            ]),
            ("key", "A ring counter gives n states from n flip-flops and a Johnson counter gives 2n, against 2ⁿ for a binary counter. They trade density for decoding that needs no gates."),
        ],
    },

    "dl_sync": {
        "title": "Synchronous Sequential Circuits",
        "blocks": [
            ("p", "All flip-flops share one clock, so the whole circuit moves between states at a single instant."),
            ("steps", [
                "Draw the state diagram and state table.",
                "Assign binary codes to states.",
                "Choose a flip-flop type and use its excitation table to find the input equations.",
                "Minimise those equations with K-maps.",
            ]),
            ("table", [
                ["Flip-flop", "Excitation for Q → Q⁺"],
                ["D", "D = Q⁺"],
                ["T", "T = Q ⊕ Q⁺"],
                ["JK", "J and K, one may be don't-care"],
            ]),
            ("key", "The JK excitation table has a don't-care in every row, which is why JK-based designs usually minimise to fewer gates than D-based ones."),
            ("note", "The maximum clock frequency is set by the longest path: flip-flop propagation delay plus combinational delay plus setup time."),
        ],
    },
}
