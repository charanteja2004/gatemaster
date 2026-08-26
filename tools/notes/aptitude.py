# -*- coding: utf-8 -*-
"""General Aptitude revision notes. Fifteen marks in every GATE paper."""

NOTES = {
    "aptitude_quantitative": {
        "title": "Quantitative Aptitude",
        "blocks": [
            ("p", "Fifteen marks of every GATE paper are General Aptitude, and roughly half of that is arithmetic. The topics are few and the formulas are fixed, so the marks are the most reliable in the exam."),
            ("h", "Averages and ratios"),
            ("formula", "Average = sum / count;  sum = average × count"),
            ("formula", "If a : b = m : n, write a = mk and b = nk and solve for k"),
            ("example", "Five numbers average 20, so they total 100. Remove one and the remaining four average 22, totalling 88 — the number removed was 12."),
            ("key", "Convert averages to totals immediately. Almost every average question becomes one line of arithmetic once you stop working with the average itself."),
            ("h", "Ages"),
            ("example", "Ages in ratio 3 : 4 now and 4 : 5 in six years: (3x+6)/(4x+6) = 4/5 gives x = 6, so the ages are 18 and 24."),
        ],
    },

    "aptitude_numbers": {
        "title": "Numbers",
        "blocks": [
            ("formula", "HCF × LCM = product of the two numbers"),
            ("bullets", [
                "Divisible by 3 or 9: the digit sum is.",
                "Divisible by 4: the last two digits are.",
                "Divisible by 8: the last three digits are.",
                "Divisible by 11: the alternating digit sum is.",
            ]),
            ("h", "Remainders"),
            ("p", "For powers, reduce the base modulo the divisor first, then find the cycle length of the remainders."),
            ("example", "7¹⁰⁰ mod 5: 7 ≡ 2, and powers of 2 cycle 2, 4, 3, 1 with period 4. Since 100 is a multiple of 4, the remainder is 1."),
            ("key", "A number leaving remainder r with several divisors is r more than a common multiple. The least such number is LCM + r — for 12, 15 and 20 with r = 3 that is 63."),
        ],
    },

    "aptitude_percentage": {
        "title": "Percentages",
        "blocks": [
            ("formula", "Successive changes multiply: a rise of x% then a fall of y% gives a net factor of (1 + x/100)(1 − y/100)"),
            ("example", "Up 20% then down 20% gives 1.2 × 0.8 = 0.96, a net fall of 4%. The two do not cancel because the second percentage is taken on a larger base."),
            ("formula", "If A is x% more than B, then B is (100x)/(100 + x)% less than A"),
            ("example", "A is 25% more than B, so B is 2500/125 = 20% less than A, not 25%."),
            ("h", "Price and consumption"),
            ("formula", "Price rises by x% ⟹ consumption must fall by (100x)/(100 + x)% to keep expenditure fixed"),
            ("key", "Read which quantity the percentage is taken on. Nearly every wrong answer in this topic comes from using the wrong base."),
        ],
    },

    "aptitude_profit": {
        "title": "Profit and Loss",
        "blocks": [
            ("formula", "Profit% = (SP − CP)/CP × 100;  Loss% = (CP − SP)/CP × 100"),
            ("formula", "SP = CP × (100 + profit%)/100"),
            ("formula", "SP after discount = MP × (100 − discount%)/100"),
            ("example", "Marked 40% above cost with a 25% discount: 140 × 0.75 = 105, so a 5% profit on a cost of 100."),
            ("key", "Profit percentage is always on cost price and discount is always on marked price. Mixing the two bases is the standard trap."),
            ("warn", "Selling two articles at the same price, one at x% profit and one at x% loss, always gives an overall loss of x²/100 percent — never break-even."),
        ],
    },

    "aptitude_simple_intrest": {
        "title": "Simple Interest",
        "blocks": [
            ("formula", "SI = P × R × T / 100;  Amount = P + SI"),
            ("formula", "Compound interest: A = P(1 + R/100)ᵀ"),
            ("example", "A sum doubling in 8 years under simple interest earns interest equal to P in 8 years, so it needs 16 years to earn 2P and triple."),
            ("key", "Under simple interest the yearly interest is constant, so amounts at different times differ by a fixed step. Amounts of 8100 after 3 years and 8400 after 4 give 300 a year and a principal of 7200."),
            ("note", "Simple and compound interest are equal for the first year. The difference after two years is P(R/100)², which is a one-line shortcut worth remembering."),
        ],
    },

    "aptitude_progression": {
        "title": "Arithmetic Progression",
        "blocks": [
            ("formula", "AP: aₙ = a + (n − 1)d;  Sₙ = n/2 × (2a + (n − 1)d) = n/2 × (first + last)"),
            ("formula", "GP: aₙ = a·rⁿ⁻¹;  Sₙ = a(rⁿ − 1)/(r − 1);  S∞ = a/(1 − r) for |r| < 1"),
            ("example", "First term 5, common difference 3, twenty terms: S = 10 × (10 + 57) = 670."),
            ("bullets", [
                "Sum of the first n natural numbers: n(n+1)/2.",
                "Sum of their squares: n(n+1)(2n+1)/6.",
                "Sum of their cubes: [n(n+1)/2]².",
            ]),
            ("key", "Given two terms, subtract to find d: aₚ − a_q = (p − q)d. That one step solves most AP questions without finding the first term at all."),
        ],
    },

    "aptitude_speed": {
        "title": "Speed, Time and Distance",
        "blocks": [
            ("formula", "Distance = speed × time"),
            ("formula", "km/h to m/s: multiply by 5/18.  m/s to km/h: multiply by 18/5"),
            ("formula", "Average speed over equal distances = 2xy/(x + y)"),
            ("example", "40 km/h out and 60 km/h back gives 2×40×60/100 = 48 km/h, not 50. The slower leg takes longer, so it weighs more."),
            ("h", "Trains"),
            ("bullets", [
                "Crossing a pole: distance is the train's length.",
                "Crossing a platform or bridge: distance is train length + platform length.",
                "Two trains in opposite directions: speeds add. Same direction: speeds subtract.",
            ]),
            ("example", "A 150 m train at 90 km/h (25 m/s) crossing a 300 m bridge covers 450 m in 18 s."),
            ("h", "Boats"),
            ("formula", "Downstream = boat + stream;  upstream = boat − stream"),
        ],
    },

    "aptitude_work": {
        "title": "Work and Wages",
        "blocks": [
            ("formula", "If A finishes in a days, A's rate is 1/a of the work per day"),
            ("formula", "Together: 1/a + 1/b = 1/t, so t = ab/(a + b)"),
            ("example", "12 days and 18 days together: 1/12 + 1/18 = 5/36, so 36/5 = 7.2 days."),
            ("h", "Man-days"),
            ("formula", "M₁ × D₁ × H₁ / W₁ = M₂ × D₂ × H₂ / W₂"),
            ("example", "12 men for 20 days is 240 man-days. After 8 days, 96 are done and 144 remain; with 8 men left that is 18 more days, so 26 in total."),
            ("key", "Work as a total of man-days handles every joining-and-leaving variation. Rates as fractions handle every together-and-alone variation. Pick whichever matches the question."),
            ("note", "Negative work is the same arithmetic: someone undoing work has a negative rate, which is exactly how an outlet pipe is modelled."),
        ],
    },

    "aptitude_pipes": {
        "title": "Pipes and Cisterns",
        "blocks": [
            ("p", "Identical to work and time: a filling pipe has a positive rate and an emptying pipe a negative one."),
            ("formula", "Net rate = Σ filling rates − Σ emptying rates;  time = 1 / net rate"),
            ("example", "Filling in 10 h against emptying in 15 h: 1/10 − 1/15 = 1/30, so the tank fills in 30 hours."),
            ("example", "A (20 min) and B (30 min) open together, A closed after 5 min: 5 × (1/20 + 1/30) = 5/12 filled, and B finishes the remaining 7/12 in 17.5 min — 22.5 min in total."),
            ("key", "If the emptying rate exceeds the filling rate the tank never fills, and the question is testing whether you check the sign."),
        ],
    },

    "aptitude_clock": {
        "title": "Clocks",
        "blocks": [
            ("bullets", [
                "The minute hand moves 6° per minute.",
                "The hour hand moves 0.5° per minute, or 30° per hour.",
                "The minute hand gains 5.5° per minute on the hour hand.",
            ]),
            ("formula", "Angle = |30H − 5.5M| degrees, taking 360 − θ if the result exceeds 180"),
            ("example", "At 4:20 the angle is |120 − 110| = 10°, not 0 — the hour hand has already drifted a third of the way to 5."),
            ("bullets", [
                "The hands coincide 11 times in 12 hours, so 22 times a day.",
                "They are opposite 11 times in 12 hours, and at right angles 22 times.",
            ]),
            ("key", "There is no coincidence between 11 and 12 o'clock — the 11 o'clock and 12 o'clock cases are the same event, which is why the count is 11 and not 12."),
        ],
    },

    "aptitude_probability": {
        "title": "Probability",
        "blocks": [
            ("formula", "P(E) = favourable outcomes / total outcomes,  0 ≤ P(E) ≤ 1"),
            ("formula", "P(A ∪ B) = P(A) + P(B) − P(A ∩ B)"),
            ("formula", "Independent events: P(A ∩ B) = P(A)·P(B)"),
            ("formula", "Conditional: P(A|B) = P(A ∩ B) / P(B)"),
            ("h", "Standard counts"),
            ("bullets", [
                "Two dice: 36 outcomes; a sum of 7 occurs 6 ways, so P = 1/6.",
                "Three coins: 8 outcomes; exactly two heads occurs 3 ways, so P = 3/8.",
                "Drawing without replacement: the denominator shrinks each draw.",
            ]),
            ("example", "4 red and 6 blue, two drawn without replacement: (4/10) × (3/9) = 2/15 ≈ 0.13."),
            ("warn", "Mutually exclusive and independent are different. Mutually exclusive events with non-zero probability are never independent — if one happens the other cannot."),
        ],
    },

    "aptitude_2d": {
        "title": "Data Interpretation",
        "blocks": [
            ("p", "A table, bar chart or pie chart with questions about totals, percentages and growth. The arithmetic is easy; the reading is where marks are lost."),
            ("steps", [
                "Read the units and the scale before anything else.",
                "Note whether values are absolute or percentages.",
                "Compute totals once and reuse them.",
                "Approximate when the options are far apart.",
            ]),
            ("formula", "Percentage change = (new − old)/old × 100"),
            ("example", "Sales 40, 50, 45, 60 over four years: the growth from the first to the last is 20/40 = 50%, while the largest year-on-year rise is 15/45 = 33.3%."),
            ("key", "In a pie chart each 1% is 3.6°. Questions frequently give degrees and expect a value, or the reverse."),
            ("warn", "The largest percentage increase and the largest absolute increase need not be the same year. Check which one is asked."),
        ],
    },

    "aptitude_puzzle": {
        "title": "Puzzles",
        "blocks": [
            ("p", "A set of constraints and a set of items to arrange. The method is always the same: fix what is certain, then eliminate."),
            ("steps", [
                "Draw a grid of items against attributes.",
                "Mark the definite statements first.",
                "Cross out what each negative statement forbids.",
                "Look for a row or column with one possibility left, and repeat.",
            ]),
            ("key", "Start with the most restrictive clue, not the first one. A statement fixing an exact position is worth more than one giving a relation."),
            ("note", "Never carry an assumption silently. If a branch is needed, note it and check the contradiction, otherwise a wrong guess propagates unnoticed."),
        ],
    },

    "aptitude_seating_arrangement": {
        "title": "Seating Arrangement",
        "blocks": [
            ("bullets", [
                "Linear: everyone faces the same way unless stated. Left and right are the seated person's.",
                "Circular facing the centre: clockwise is a person's left.",
                "Circular facing outward: clockwise is a person's right.",
            ]),
            ("key", "The facing direction reverses left and right. Half the errors in this topic come from applying the arranger's viewpoint instead of the seated person's."),
            ("example", "Five in a row: A immediately left of B, D between B and E, and C at one end forces the block A B D E, so the order is C A B D E and E is at the other end."),
            ("note", "With n people in a circle, the person opposite is n/2 seats away, which only exists when n is even."),
        ],
    },

    "aptitude_order": {
        "title": "Ranking and Order",
        "blocks": [
            ("formula", "Total = position from left + position from right − 1"),
            ("formula", "Position from right = total − position from left + 1"),
            ("example", "11th from the left and 15th from the right gives 11 + 15 − 1 = 25 in the row."),
            ("example", "In a row of 40, the 12th from the left is the 29th from the right."),
            ("key", "The −1 exists because the person is counted from both ends. Adding the two positions without it is the single most common error in this topic."),
            ("warn", "If the two counts overlap — the totals do not agree — the question is describing two different people, or people are being counted between rather than including."),
        ],
    },

    "aptitude_inequality_reasoning": {
        "title": "Inequality Reasoning",
        "blocks": [
            ("p", "A chain of relations, followed by conclusions to accept or reject."),
            ("bullets", [
                "A chain combines only when every link points the same way.",
                "A chain with at least one strict link gives a strict conclusion.",
                "A chain of all ≥ gives only ≥, never >.",
            ]),
            ("example", "A > B ≥ C > D gives A > D. But M ≥ N > O ≤ P gives M > O and nothing at all about M against P, because the direction reverses at O."),
            ("key", "Equality is transparent: in P ≥ Q = R, substitute R for Q freely, so P ≥ R follows."),
            ("warn", "Either-or conclusions are true when the two together cover every case and neither holds alone — typically a pair like X ≥ Y and X < Y."),
        ],
    },

    "aptitude_alphanumeric": {
        "title": "Alphanumeric Series",
        "blocks": [
            ("steps", [
                "Take first differences; if they are constant, the series is linear.",
                "If the differences themselves form a pattern, take second differences.",
                "Check ratios for a geometric series.",
                "Check squares, cubes, primes, and n² ± n.",
            ]),
            ("example", "2, 6, 12, 20, 30 has differences 4, 6, 8, 10, so the next term is 42. Equivalently the nth term is n(n+1)."),
            ("example", "3, 7, 15, 31, 63 doubles and adds one each time, giving 127 — the terms are 2ⁿ − 1."),
            ("h", "Letter series"),
            ("bullets", [
                "Write letter positions as numbers: A = 1 … Z = 26.",
                "Opposite letters sum to 27: A ↔ Z, B ↔ Y, C ↔ X.",
            ]),
            ("example", "AZ, BY, CX continues DW — the first letter advances while the second is its mirror."),
        ],
    },

    "aptitude_verbal": {
        "title": "Verbal Ability",
        "blocks": [
            ("p", "The verbal half of General Aptitude tests grammar, vocabulary in context, and reading comprehension. It rewards precision rather than reading speed."),
            ("bullets", [
                "Subject-verb agreement, articles, determiners and voice.",
                "Word meanings from context, including near-synonyms.",
                "Sentence completion and error spotting.",
                "Reading comprehension: inference, tone and main idea.",
            ]),
            ("key", "Eliminate rather than select. Three options usually break a definite rule, and finding the broken rule is faster than justifying the survivor."),
        ],
    },

    "aptitude_grammer": {
        "title": "Grammar Basics",
        "blocks": [
            ("table", [
                ["Part of speech", "Does"],
                ["Noun", "names"],
                ["Pronoun", "replaces a noun"],
                ["Verb", "states action or being"],
                ["Adjective", "describes a noun"],
                ["Adverb", "modifies a verb, adjective or adverb"],
                ["Preposition", "relates a noun to the rest"],
                ["Conjunction", "joins"],
            ]),
            ("bullets", [
                "A clause has a subject and a verb; a phrase does not.",
                "A sentence needs at least one independent clause.",
                "Tense must stay consistent unless the meaning requires a shift.",
            ]),
            ("key", "Find the main verb and its true subject first. Most error-spotting questions are agreement or tense problems hidden behind a long phrase."),
        ],
    },

    "aptitude_article": {
        "title": "Articles",
        "blocks": [
            ("bullets", [
                "a before a consonant sound, an before a vowel sound.",
                "the for something specific, already mentioned, or unique.",
                "No article for general plurals and most uncountable nouns.",
            ]),
            ("key", "Sound decides a against an, not spelling. an honest officer, because the h is silent; a university, because it begins with a 'yoo' sound; an MBA, because M is read 'em'."),
            ("bullets", [
                "the with rivers, seas, mountain ranges, island groups: the Ganga, the Himalayas.",
                "No the with individual mountains or lakes: Everest, Lake Victoria.",
                "the with superlatives and ordinals: the best, the first.",
            ]),
        ],
    },

    "aptitude_determiners": {
        "title": "Determiners",
        "blocks": [
            ("table", [
                ["Use with", "Countable", "Uncountable"],
                ["many / few / fewer", "yes", "no"],
                ["much / little / less", "no", "yes"],
                ["some / any / a lot of", "yes", "yes"],
            ]),
            ("key", "few means almost none; a few means some. little and a little differ the same way, and the article reverses the sense entirely."),
            ("bullets", [
                "each and every take a singular verb.",
                "both, few and several take a plural verb.",
                "either and neither are singular in formal use.",
            ]),
        ],
    },

    "aptitude_modifiers": {
        "title": "Modifiers",
        "blocks": [
            ("def", "A modifier describes another element. It must sit next to what it describes, or the sentence says something other than what was meant."),
            ("warn", "A dangling modifier has nothing to attach to: 'Walking to the station, the rain started' says the rain was walking. Give the modifier a subject: 'Walking to the station, I was caught in the rain.'"),
            ("example", "'She almost drove for six hours' means she nearly drove at all. 'She drove for almost six hours' is what was meant — only is the same trap."),
            ("key", "Place only immediately before what it limits. Moving it moves the meaning, and questions rely on exactly that."),
        ],
    },

    "aptitude_passive": {
        "title": "Active and Passive Voice",
        "blocks": [
            ("formula", "Active: subject + verb + object  ⟶  Passive: object + be + past participle + by subject"),
            ("table", [
                ["Tense", "Active", "Passive"],
                ["Simple present", "writes", "is written"],
                ["Present continuous", "is writing", "is being written"],
                ["Simple past", "wrote", "was written"],
                ["Present perfect", "has written", "has been written"],
                ["Future", "will write", "will be written"],
            ]),
            ("key", "The tense must survive the change. 'They are building a bridge' becomes 'A bridge is being built', not 'is built'."),
            ("note", "The agent is dropped when it is unknown or unimportant, which is why 'Someone has stolen my bicycle' becomes simply 'My bicycle has been stolen'."),
        ],
    },

    "aptitude_agreement": {
        "title": "Subject-Verb Agreement",
        "blocks": [
            ("bullets", [
                "each, every, either, neither, someone, everybody take a singular verb.",
                "A prepositional phrase between subject and verb never changes the number.",
                "With either…or and neither…nor, the verb agrees with the nearer subject.",
                "Collective nouns take a singular verb when acting as one unit.",
            ]),
            ("example", "'Each of the students has submitted' — the subject is each, not students."),
            ("example", "'The list of approved items is on the board' — the subject is list."),
            ("example", "'Neither the manager nor the employees were satisfied' — employees is nearer, so the verb is plural."),
            ("key", "Strike out every phrase between the subject and the verb, then read what is left. The agreement error becomes obvious once the distractor is gone."),
        ],
    },

    "aptitude_verbal_analog": {
        "title": "Verbal Analogies",
        "blocks": [
            ("p", "Identify the relationship in the first pair and apply exactly that relationship to the second."),
            ("table", [
                ["Relationship", "Example"],
                ["Worker to workplace", "Doctor : Hospital"],
                ["Maker to product", "Cartographer : Map"],
                ["Tool to user", "Scalpel : Surgeon"],
                ["Antonyms", "Ignite : Extinguish"],
                ["Part to whole", "Petal : Flower"],
                ["Degree", "Warm : Hot"],
            ]),
            ("key", "State the relationship as a sentence before looking at the options. 'A cartographer makes maps' immediately rules out Stage and Chorus for choreographer."),
            ("warn", "Keep the order. If the first pair is worker-to-tool, the answer must be worker-to-tool and not tool-to-worker."),
        ],
    },

    "aptitude_paragraph": {
        "title": "Reading Comprehension",
        "blocks": [
            ("steps", [
                "Read the passage once for the main idea, not for detail.",
                "Read the question and identify what it asks: fact, inference, tone or main idea.",
                "Return to the passage for the relevant lines.",
                "Choose the option the passage supports, not the one that is merely true.",
            ]),
            ("key", "An inference must follow from the passage alone. Outside knowledge, however correct, is the commonest wrong answer in this section."),
            ("bullets", [
                "Extreme words — always, never, all — are usually wrong in inference options.",
                "Options that restate a sentence verbatim are often traps for the main idea.",
                "Tone questions turn on adjectives and adverbs, not on the subject matter.",
            ]),
        ],
    },
}
