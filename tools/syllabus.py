#!/usr/bin/env python3
"""
GATE 2026 branch, subject and syllabus data.

Paper names and codes are the official list of 30 papers from
gate2026.iitg.ac.in. Subject breakdowns follow the published syllabus for each
paper.

Two levels of detail:
  * DETAILED branches carry a full subject list with syllabus bullets and mark
    weightage. These are the papers most candidates sit.
  * OUTLINE branches carry their subject (section) names only. They are
    selectable and honest about what is behind them rather than absent.

`folder` names an assets directory that holds notes for that subject. Only the
CS paper has notes today; every other subject renders its syllabus, which is
genuinely useful on its own -- candidates look the syllabus up constantly.

Weightage is approximate and follows the usual GATE split: General Aptitude is
15 marks in every paper, and the remaining 85 are distributed across the
technical sections.
"""

from __future__ import annotations

# General Aptitude is identical in all 30 papers.
# General Aptitude carries 15 marks in every one of the 30 papers, so these
# notes are shared by every branch rather than belonging to CS.
GENERAL_APTITUDE = dict(
    id="aptitude", name="General Aptitude", short="GA", weight=15, folder="aptitude",
    syllabus=[
        "Verbal Aptitude: basic English grammar, vocabulary, reading and comprehension, narrative sequencing",
        "Quantitative Aptitude: data interpretation, 2- and 3-dimensional plots, maps and tables; numerical computation and estimation; mensuration and geometry; elementary statistics and probability",
        "Analytical Aptitude: logic (deduction and induction), analogy, numerical relations and reasoning",
        "Spatial Aptitude: transformation of shapes — translation, rotation, scaling, mirroring, assembling and grouping; paper folding, cutting and patterns in 2 and 3 dimensions",
    ],
)

# Engineering Mathematics as it appears in most engineering papers.
ENGG_MATHS_COMMON = [
    "Linear Algebra: matrix algebra, systems of linear equations, eigenvalues and eigenvectors",
    "Calculus: functions of single variable, limit, continuity and differentiability, mean value theorems, indeterminate forms; evaluation of definite and improper integrals; double and triple integrals; partial derivatives, total derivative, Taylor series; maxima and minima; gradient, divergence and curl; vector identities; line, surface and volume integrals; theorems of Stokes, Gauss and Green",
    "Differential Equations: first order equations (linear and nonlinear); higher order linear differential equations with constant coefficients; Euler-Cauchy equation; initial and boundary value problems; Laplace transforms; solutions of heat, wave and Laplace's equations",
    "Complex Variables: analytic functions; Cauchy-Riemann equations; Cauchy's integral theorem and integral formula; Taylor and Laurent series",
    "Probability and Statistics: definitions of probability, sampling theorems, conditional probability; mean, median, mode and standard deviation; random variables; binomial, Poisson and normal distributions",
    "Numerical Methods: numerical solutions of linear and non-linear algebraic equations; integration by trapezoidal and Simpson's rule; single and multi-step methods for differential equations",
]


# ---------------------------------------------------------------------------
# Papers with a full subject breakdown
# ---------------------------------------------------------------------------

DETAILED = [
    dict(
        id="cs", code="CS", name="Computer Science & Information Technology", short="CS & IT",
        subjects=[
            GENERAL_APTITUDE,
            dict(id="maths", name="Engineering Mathematics", short="Math", weight=13, syllabus=[
                "Discrete Mathematics: propositional and first order logic; sets, relations, functions, partial orders and lattices; monoids, groups; graphs — connectivity, matching, colouring; combinatorics — counting, recurrence relations, generating functions",
                "Linear Algebra: matrices, determinants, system of linear equations, eigenvalues and eigenvectors, LU decomposition",
                "Calculus: limits, continuity and differentiability; maxima and minima; mean value theorem; integration",
                "Probability and Statistics: random variables; uniform, normal, exponential, Poisson and binomial distributions; mean, median, mode and standard deviation; conditional probability and Bayes theorem",
            ]),
            dict(id="ds", name="Programming & Data Structures", short="PDS", weight=12, folder="ds", syllabus=[
                "Programming in C",
                "Recursion",
                "Arrays, stacks, queues, linked lists, trees, binary search trees, binary heaps, graphs",
            ]),
            dict(id="os", name="Operating Systems", short="OS", weight=9, folder="os", syllabus=[
                "System calls, processes, threads, inter-process communication, concurrency and synchronization",
                "Deadlock",
                "CPU and I/O scheduling",
                "Memory management and virtual memory",
                "File systems",
            ]),
            dict(id="algo", name="Algorithms", short="Algo", weight=8, folder="algo", syllabus=[
                "Searching, sorting, hashing",
                "Asymptotic worst case time and space complexity",
                "Algorithm design techniques: greedy, dynamic programming and divide-and-conquer",
                "Graph traversals, minimum spanning trees, shortest paths",
            ]),
            dict(id="dbms", name="Databases", short="DBMS", weight=8, folder="dbms", syllabus=[
                "ER-model",
                "Relational model: relational algebra, tuple calculus, SQL",
                "Integrity constraints, normal forms",
                "File organization, indexing (e.g. B and B+ trees)",
                "Transactions and concurrency control",
            ]),
            dict(id="cao", name="Computer Organisation & Architecture", short="COA", weight=8, folder="cao", syllabus=[
                "Machine instructions and addressing modes",
                "ALU, data-path and control unit",
                "Instruction pipelining, pipeline hazards",
                "Memory hierarchy: cache, main memory and secondary storage",
                "I/O interface (interrupt and DMA mode)",
            ]),
            dict(id="toc", name="Theory of Computation", short="TOC", weight=8, folder="toc", syllabus=[
                "Regular expressions and finite automata",
                "Context-free grammars and push-down automata",
                "Regular and context-free languages, pumping lemma",
                "Turing machines and undecidability",
            ]),
            dict(id="cn", name="Computer Networks", short="CN", weight=8, syllabus=[
                "Concept of layering: OSI and TCP/IP protocol stacks; basics of packet, circuit and virtual circuit switching",
                "Data link layer: framing, error detection, medium access control, Ethernet bridging",
                "Routing protocols: shortest path, flooding, distance vector and link state routing",
                "Fragmentation and IP addressing, IPv4, CIDR notation; IP support protocols (ARP, DHCP, ICMP), NAT",
                "Transport layer: flow control and congestion control, UDP, TCP, sockets",
                "Application layer protocols: DNS, SMTP, HTTP, FTP, email",
            ]),
            dict(id="dl", name="Digital Logic", short="DL", weight=6, folder="dl", syllabus=[
                "Boolean algebra",
                "Combinational and sequential circuits",
                "Minimization",
                "Number representations and computer arithmetic (fixed and floating point)",
            ]),
            dict(id="cd", name="Compiler Design", short="CD", weight=5, folder="cd", syllabus=[
                "Lexical analysis, parsing, syntax-directed translation",
                "Runtime environments",
                "Intermediate code generation",
                "Local optimisation; data flow analyses: constant propagation, liveness analysis, common subexpression elimination",
            ]),
        ],
    ),

    dict(
        id="me", code="ME", name="Mechanical Engineering", short="Mechanical",
        subjects=[
            GENERAL_APTITUDE,
            dict(id="maths", name="Engineering Mathematics", short="Math", weight=13,
                 syllabus=ENGG_MATHS_COMMON),
            dict(id="am", name="Applied Mechanics & Design", short="AMD", weight=25, syllabus=[
                "Engineering Mechanics: free-body diagrams and equilibrium; friction and its applications; kinematics and dynamics of rigid bodies; impulse and momentum; energy methods; principles of vibrations",
                "Mechanics of Materials: stress and strain, elastic constants, Mohr's circle; shear force and bending moment diagrams; bending and shear stresses; deflection of beams; torsion of circular shafts; thin cylinders; columns and struts; strain energy methods; thermal stresses",
                "Theory of Machines: displacement, velocity and acceleration analysis of plane mechanisms; dynamic analysis of linkages; cams; gears and gear trains; flywheels and governors; balancing of reciprocating and rotating masses; gyroscope",
                "Vibrations: free and forced vibration of single degree of freedom systems, effect of damping; vibration isolation; resonance; critical speeds of shafts",
                "Machine Design: design for static and dynamic loading; failure theories; fatigue strength and the S-N diagram; principles of the design of machine elements such as bolted, riveted and welded joints, shafts, gears, rolling and sliding contact bearings, brakes and clutches, springs",
            ]),
            dict(id="ft", name="Fluid Mechanics & Thermal Sciences", short="FMTS", weight=25, syllabus=[
                "Fluid Mechanics: fluid properties; fluid statics, forces on submerged bodies, stability of floating bodies; control-volume analysis of mass, momentum and energy; fluid acceleration; differential equations of continuity and momentum; Bernoulli's equation; dimensional analysis; flow through pipes, head losses in pipes and bends",
                "Heat Transfer: modes of heat transfer; one-dimensional heat conduction, resistance concept and electrical analogy, heat transfer through fins; unsteady heat conduction, lumped parameter system; dimensionless parameters in free and forced convective heat transfer; heat exchanger performance, LMTD and NTU methods; radiative heat transfer, Stefan-Boltzmann law, Wien's displacement law, black and grey surfaces, view factors, radiation network analysis",
                "Thermodynamics: thermodynamic systems and processes; properties of pure substances, behaviour of ideal and real gases; zeroth and first laws of thermodynamics; second law of thermodynamics; irreversibility and availability; behaviour of moist air; thermodynamic relations",
                "Applications: power engineering — air and gas compressors, vapour and gas power cycles, concepts of regeneration and reheat; I.C. engines — air-standard Otto, Diesel and dual cycles; refrigeration and air-conditioning — vapour and gas refrigeration and heat pump cycles, psychrometry; turbomachinery — impulse and reaction principles, velocity diagrams, Pelton wheel, Francis and Kaplan turbines",
            ]),
            dict(id="mm", name="Materials, Manufacturing & Industrial Engineering", short="MMIE", weight=22, syllabus=[
                "Engineering Materials: structure and properties of engineering materials; phase diagrams; heat treatment; stress-strain diagrams for engineering materials",
                "Casting, Forming and Joining: different types of castings, design of patterns, moulds and cores, solidification and cooling, riser and gating design; plastic deformation and yield criteria; fundamentals of hot and cold working; load estimation for bulk and sheet forming; principles of powder metallurgy; principles of welding, brazing, soldering and adhesive bonding",
                "Machining and Machine Tool Operations: mechanics of machining; basic machine tools; single and multi-point cutting tools, tool geometry and materials, tool life and wear; economics of machining; non-traditional machining processes",
                "Metrology and Inspection: limits, fits and tolerances; linear and angular measurements; comparators; interferometry; form and finish measurement; alignment and testing methods; tolerance analysis in manufacturing and assembly",
                "Computer Integrated Manufacturing: basic concepts of CAD/CAM and their integration tools; additive manufacturing",
                "Production Planning and Control: forecasting models, aggregate production planning, scheduling, materials requirement planning; lean manufacturing",
                "Inventory Control: deterministic models; safety stock inventory control systems",
                "Operations Research: linear programming, simplex method, transportation, assignment, network flow models, simple queuing models, PERT and CPM",
            ]),
        ],
    ),

    dict(
        id="ee", code="EE", name="Electrical Engineering", short="Electrical",
        subjects=[
            GENERAL_APTITUDE,
            dict(id="maths", name="Engineering Mathematics", short="Math", weight=13,
                 syllabus=ENGG_MATHS_COMMON),
            dict(id="ec", name="Electric Circuits", short="Circuits", weight=10, syllabus=[
                "Network elements: ideal voltage and current sources, dependent sources, R, L, C, M elements",
                "Network solution methods: KCL, KVL, node and mesh analysis",
                "Network theorems: Thevenin's, Norton's, superposition and maximum power transfer theorem",
                "Transient response of DC and AC networks; sinusoidal steady-state analysis; resonance; two-port networks; balanced three-phase circuits; star-delta transformation; complex power and power factor in AC circuits",
            ]),
            dict(id="ef", name="Electromagnetic Fields", short="EMF", weight=6, syllabus=[
                "Coulomb's law, electric field intensity, electric flux density, Gauss's law, divergence, electric field and potential due to point, line, plane and spherical charge distributions",
                "Effect of dielectric medium, capacitance of simple configurations",
                "Biot-Savart's law, Ampere's law, curl, Faraday's law, Lorentz force, inductance, magnetomotive force, reluctance, magnetic circuits",
                "Self and mutual inductance of simple configurations",
            ]),
            dict(id="ss", name="Signals & Systems", short="S&S", weight=8, syllabus=[
                "Representation of continuous and discrete time signals; shifting and scaling properties; linear time invariant and causal systems",
                "Fourier series representation of continuous and discrete time periodic signals",
                "Sampling theorem; applications of Fourier transform for continuous and discrete time signals",
                "Laplace transform and z-transform",
            ]),
            dict(id="em", name="Electrical Machines", short="Machines", weight=12, syllabus=[
                "Single phase transformer: equivalent circuit, phasor diagram, open circuit and short circuit tests, regulation and efficiency",
                "Three phase transformers: connections, vector groups, parallel operation; auto-transformer; electromechanical energy conversion principles",
                "DC machines: separately excited, series and shunt, motoring and generating mode of operation and their characteristics, speed control of DC motors",
                "Three phase induction motors: principle of operation, types, performance, torque-speed characteristics, no-load and blocked rotor tests, equivalent circuit, starting and speed control",
                "Operating principle of single phase induction motors; synchronous machines: cylindrical and salient pole machines, performance and characteristics, regulation and parallel operation of generators, starting of synchronous motors",
            ]),
            dict(id="ps", name="Power Systems", short="Power", weight=12, syllabus=[
                "Basic concepts of electrical power generation, AC and DC transmission concepts, models and performance of transmission lines and cables",
                "Economic load dispatch (with and without considering transmission losses); series and shunt compensation, electric field distribution and insulators",
                "Distribution systems, per-unit quantities, bus admittance matrix, GaussSeidel and Newton-Raphson load flow methods, voltage and frequency control, power factor correction",
                "Symmetrical components, symmetrical and unsymmetrical fault analysis, principles of over-current, differential, directional and distance protection; circuit breakers",
                "System stability concepts, equal area criterion",
            ]),
            dict(id="cs_ctrl", name="Control Systems", short="Control", weight=9, syllabus=[
                "Mathematical modelling and representation of systems, feedback principle, transfer function, block diagram representation, signal flow graph",
                "Transient and steady-state analysis of linear time invariant systems, stability analysis using Routh-Hurwitz and Nyquist criteria, Bode plots, root loci",
                "Lag, lead and lead-lag compensators; P, PI and PID controllers",
                "State space model, solution of state equations of LTI systems",
            ]),
            dict(id="meas", name="Electrical & Electronic Measurements", short="Meas", weight=6, syllabus=[
                "Bridges and potentiometers; measurement of voltage, current, power, energy and power factor",
                "Instrument transformers, digital voltmeters and multimeters, phase, time and frequency measurement, oscilloscopes, error analysis",
            ]),
            dict(id="ade", name="Analog & Digital Electronics", short="Electronics", weight=9, syllabus=[
                "Diode circuits: clipping, clamping and rectifiers; BJT and MOSFET amplifiers: biasing, AC coupling, small signal analysis, frequency response",
                "Op-amp circuits: amplifiers, summers, differentiators, integrators, active filters, Schmitt triggers and oscillators",
                "Combinatorial and sequential logic circuits, multiplexers, demultiplexers, Schmitt triggers, sample and hold circuits, A/D and D/A converters",
            ]),
            dict(id="pe", name="Power Electronics", short="PE", weight=10, syllabus=[
                "Static V-I characteristics and firing/gating circuits for thyristor, MOSFET and IGBT",
                "DC to DC conversion: buck, boost and buck-boost converters; single and three phase configuration of uncontrolled rectifiers",
                "Voltage and current commutated thyristor based converters; bidirectional AC to DC voltage source converters; magnitude and phase of line current harmonics for uncontrolled and thyristor based converters",
                "Power factor and distortion factor of AC to DC converters; single phase and three phase voltage source inverters; sinusoidal pulse width modulation",
            ]),
        ],
    ),

    dict(
        id="ec", code="EC", name="Electronics & Communication Engineering", short="ECE",
        subjects=[
            GENERAL_APTITUDE,
            dict(id="maths", name="Engineering Mathematics", short="Math", weight=13, syllabus=[
                "Linear Algebra: vector space, basis, linear dependence and independence, matrix algebra, eigenvalues and eigenvectors, rank, solution of linear equations — existence and uniqueness",
                "Calculus: mean value theorems, theorems of integral calculus, evaluation of definite and improper integrals, partial derivatives, maxima and minima, multiple integrals, line, surface and volume integrals, Taylor series",
                "Differential Equations: first order equations (linear and nonlinear), higher order linear differential equations, Cauchy's and Euler's equations, methods of solution using variation of parameters, complementary function and particular integral, partial differential equations, variable separable method, initial and boundary value problems",
                "Vector Analysis: vectors in plane and space, vector operations, gradient, divergence and curl, Gauss's, Green's and Stokes' theorems",
                "Complex Analysis: analytic functions, Cauchy's integral theorem, Cauchy's integral formula, sequences, series, convergence tests, Taylor and Laurent series, residue theorem",
                "Probability and Statistics: mean, median, mode, standard deviation, combinatorial probability, probability distributions, binomial, Poisson, exponential and normal distribution, joint and conditional probability",
            ]),
            dict(id="nss", name="Networks, Signals & Systems", short="NSS", weight=14, syllabus=[
                "Circuit analysis: node and mesh analysis, superposition, Thevenin's theorem, Norton's theorem, reciprocity; sinusoidal steady state analysis: phasors, complex power, maximum power transfer",
                "Time and frequency domain analysis of linear circuits: RL, RC and RLC circuits, solution of network equations using Laplace transform",
                "Linear 2-port network parameters, wye-delta transformation",
                "Continuous-time signals: Fourier series and Fourier transform, sampling theorem and applications",
                "Discrete-time signals: DTFT, DFT, z-transform, discrete-time processing of continuous-time signals; LTI systems: definition and properties, causality, stability, impulse response, convolution, poles and zeroes, frequency response, group delay, phase delay",
            ]),
            dict(id="ed", name="Electronic Devices", short="Devices", weight=8, syllabus=[
                "Energy bands in intrinsic and extrinsic semiconductors, equilibrium carrier concentration, direct and indirect band-gap semiconductors",
                "Carrier transport: diffusion current, drift current, mobility and resistivity, generation and recombination of carriers, Poisson and continuity equations",
                "P-N junction, Zener diode, BJT, MOS capacitor, MOSFET, LED, photo diode and solar cell",
            ]),
            dict(id="ac", name="Analog Circuits", short="Analog", weight=12, syllabus=[
                "Diode circuits: clipping, clamping and rectifiers",
                "BJT and MOSFET amplifiers: biasing, AC coupling, small signal analysis, frequency response",
                "Current mirrors and differential amplifiers",
                "Op-amp circuits: amplifiers, summers, differentiators, integrators, active filters, Schmitt triggers and oscillators",
            ]),
            dict(id="dc", name="Digital Circuits", short="Digital", weight=12, syllabus=[
                "Number representations: binary, integer and floating-point numbers",
                "Combinatorial circuits: Boolean algebra, minimization of functions using Boolean identities and Karnaugh map, logic gates and their static CMOS implementations, arithmetic circuits, code converters, multiplexers, decoders",
                "Sequential circuits: latches and flip-flops, counters, shift-registers, finite state machines, propagation delay, setup and hold time, critical path delay",
                "Data converters: sample and hold circuits, ADCs and DACs",
                "Semiconductor memories: ROM, SRAM, DRAM; computer organization: machine instructions and addressing modes, ALU, data-path and control unit, instruction pipelining",
            ]),
            dict(id="cs_ctrl", name="Control Systems", short="Control", weight=10, syllabus=[
                "Basic control system components; feedback principle; transfer function; block diagram representation; signal flow graph; transient and steady-state analysis of LTI systems; frequency response",
                "Routh-Hurwitz and Nyquist stability criteria; Bode and root-locus plots; lag, lead and lag-lead compensation; state variable model and solution of state equation of LTI systems",
            ]),
            dict(id="comm", name="Communications", short="Comm", weight=12, syllabus=[
                "Random processes: autocorrelation and power spectral density, properties of white noise, filtering of random signals through LTI systems",
                "Analog communications: amplitude modulation and demodulation, angle modulation and demodulation, spectra of AM and FM, superheterodyne receivers",
                "Information theory: entropy, mutual information and channel capacity theorem",
                "Digital communications: PCM, DPCM, digital modulation schemes (ASK, PSK, FSK, QAM), bandwidth, inter-symbol interference, MAP, ML detection, matched filter receiver, SNR and BER",
                "Fundamentals of error correction, Hamming codes, CRC",
            ]),
            dict(id="emag", name="Electromagnetics", short="EM", weight=9, syllabus=[
                "Maxwell's equations: differential and integral forms and their interpretation, boundary conditions, wave equation, Poynting vector",
                "Plane waves and properties: reflection and refraction, polarization, phase and group velocity, propagation through various media, skin depth",
                "Transmission lines: equations, characteristic impedance, impedance matching, impedance transformation, S-parameters, Smith chart",
                "Rectangular and circular waveguides, light propagation in optical fibers, dipole and monopole antennas, linear antenna arrays",
            ]),
        ],
    ),

    dict(
        id="ce", code="CE", name="Civil Engineering", short="Civil",
        subjects=[
            GENERAL_APTITUDE,
            dict(id="maths", name="Engineering Mathematics", short="Math", weight=13,
                 syllabus=ENGG_MATHS_COMMON),
            dict(id="se", name="Structural Engineering", short="Structural", weight=25, syllabus=[
                "Engineering Mechanics: system of forces, free-body diagrams, equilibrium equations; internal forces in structures; friction and its applications; kinematics of point mass and rigid body; centre of mass; Euler's equations of motion; impulse-momentum; energy methods; principles of virtual work",
                "Solid Mechanics: bending moment and shear force in statically determinate beams; simple stress and strain relationships; simple bending theory, flexural and shear stresses, shear centre; uniform torsion, transformation of stress, buckling of columns, combined and direct bending stresses",
                "Structural Analysis: statically determinate and indeterminate structures by force/energy methods; method of superposition; analysis of trusses, arches, beams, cables and frames; displacement methods — slope deflection and moment distribution methods; influence lines; stiffness and flexibility methods of structural analysis",
                "Construction Materials and Management: construction materials — structural steel, concrete; construction management — types of construction projects, project planning and network analysis (PERT and CPM), cost estimation",
                "Concrete Structures: working stress and limit state design concepts; design of beams, slabs, columns; bond and development length; prestressed concrete beams",
                "Steel Structures: working stress and limit state design concepts; design of tension and compression members, beams and beam-columns, column bases; connections — simple and eccentric, beam-column connections, plate girders and trusses; concept of plastic analysis of beams and frames",
            ]),
            dict(id="ge", name="Geotechnical Engineering", short="Geotech", weight=17, syllabus=[
                "Soil Mechanics: three-phase system and phase relationships, index properties; unified and Indian standard soil classification system; permeability — one dimensional flow, seepage through soils, two-dimensional flow, flow nets, uplift pressure, piping, capillarity, seepage force; principle of effective stress and quicksand condition; compaction of soils; one-dimensional consolidation, time rate of consolidation; shear strength, Mohr's circle, effective and total shear strength parameters, stress-strain characteristics of clays and sand",
                "Foundation Engineering: sub-surface investigations — drilling boreholes, sampling, plate load test, standard penetration and cone penetration tests; earth pressure theories — Rankine and Coulomb; stability of slopes — finite and infinite slopes, Bishop's method; stress distribution in soils — Boussinesq's theory; pressure bulbs, shallow foundations — Terzaghi's and Meyerhoff's bearing capacity theories, effect of water table; combined footing and raft foundation; contact pressure; settlement analysis in sands and clays; deep foundations — dynamic and static formulae, load capacity of piles in sands and clays, pile load test, negative skin friction",
            ]),
            dict(id="wr", name="Water Resources Engineering", short="Water", weight=16, syllabus=[
                "Fluid Mechanics: properties of fluids, fluid statics; continuity, momentum and energy equations and their applications; potential flow, laminar and turbulent flow, flow in pipes, pipe networks; concept of boundary layer and its growth; concept of lift and drag",
                "Hydraulics: forces on immersed bodies; flow measurement in channels and pipes; dimensional analysis and hydraulic similitude; channel hydraulics — energy-depth relationships, specific energy, critical flow, hydraulic jump, uniform flow, gradually varied flow and water surface profiles",
                "Hydrology: hydrologic cycle, precipitation, evaporation, evapo-transpiration, watershed, infiltration, unit hydrographs, hydrograph analysis, reservoir capacity, flood estimation and routing, surface run-off models, ground water hydrology — steady state well hydraulics and aquifers; application of Darcy's law",
                "Irrigation: types of irrigation systems and methods; crop water requirements — duty, delta, evapo-transpiration; gravity dams and spillways; lined and unlined canals, design of weirs on permeable foundation; cross drainage structures",
            ]),
            dict(id="env", name="Environmental Engineering", short="Environ", weight=12, syllabus=[
                "Water and Waste Water Quality and Treatment: basics of water quality standards — physical, chemical and biological parameters; water quality index; unit processes and operations; water requirement; water distribution system; drinking water treatment; sewerage system design, quantity of domestic wastewater, primary and secondary treatment; effluent discharge standards; sludge disposal; reuse of treated sewage for different applications",
                "Air Pollution: types of pollutants, their sources and impacts, air pollution control, air quality standards, air quality index and limits",
                "Municipal Solid Wastes: characteristics, generation, collection and transportation of solid wastes, engineered systems for solid waste management (reuse/recycle, energy recovery, treatment and disposal)",
            ]),
            dict(id="te", name="Transportation Engineering", short="Transport", weight=9, syllabus=[
                "Transportation Infrastructure: geometric design of highways — cross-sectional elements, sight distances, horizontal and vertical alignments; geometric design of railway track; airport runway length, taxiway and exit taxiway design",
                "Highway Pavements: highway materials — desirable properties and quality control tests; design of bituminous paving mixes; design of flexible and rigid pavements",
                "Traffic Engineering: traffic studies on flow and speed, peak hour factor, accident study, statistical analysis of traffic data; microscopic and macroscopic parameters of traffic flow, fundamental relationships; traffic signs; signal design by Webster's method; types of intersections; highway capacity",
            ]),
            dict(id="gm", name="Geomatics Engineering", short="Geomatics", weight=8, syllabus=[
                "Principles of surveying; errors and their adjustment; maps — scale, coordinate system; distance and angle measurement — levelling and trigonometric levelling; traversing and triangulation survey; total station; horizontal and vertical curves",
                "Photogrammetry and remote sensing — scale, flying height; basics of remote sensing and GIS",
            ]),
        ],
    ),

    dict(
        id="ch", code="CH", name="Chemical Engineering", short="Chemical",
        subjects=[
            GENERAL_APTITUDE,
            dict(id="maths", name="Engineering Mathematics", short="Math", weight=13,
                 syllabus=ENGG_MATHS_COMMON),
            dict(id="pct", name="Process Calculations & Thermodynamics", short="PCT", weight=12, syllabus=[
                "Steady and unsteady state mass and energy balances including multiphase, multi-component, reacting and non-reacting systems; use of tie components; recycle, bypass and purge calculations; degrees of freedom analysis",
                "First and second laws of thermodynamics; applications of first law to close and open systems; second law and entropy; thermodynamic properties of pure substances — equation of state and departure function, properties of mixtures — partial molar properties, fugacity, excess properties and activity coefficients; phase equilibria — predicting VLE of systems; chemical reaction equilibria",
            ]),
            dict(id="fm", name="Fluid Mechanics & Mechanical Operations", short="FMMO", weight=12, syllabus=[
                "Fluid statics, Newtonian and non-Newtonian fluids, shell balances including differential form of Bernoulli equation and energy balance, macroscopic friction factors, dimensional analysis and similitude, flow through pipeline systems, flow meters, pumps and compressors, elementary boundary layer theory, flow past immersed bodies including packed and fluidized beds, turbulent flow — fluctuating velocity, universal velocity profile and pressure drop",
                "Particle size and shape, particle size distribution, size reduction and classification of solid particles; free and hindered settling; centrifuge and cyclones; thickening and classification, filtration, agitation and mixing; conveying of solids",
            ]),
            dict(id="ht", name="Heat Transfer", short="Heat", weight=10, syllabus=[
                "Steady and unsteady heat conduction, convection and radiation, thermal boundary layer and heat transfer coefficients, boiling, condensation and evaporation; types of heat exchangers and evaporators and their process calculations; design of double pipe, shell and tube heat exchangers, and single and multiple effect evaporators",
            ]),
            dict(id="mt", name="Mass Transfer", short="Mass", weight=12, syllabus=[
                "Fick's laws, molecular diffusion in fluids, mass transfer coefficients, film, penetration and surface renewal theories; momentum, heat and mass transfer analogies; stage-wise and continuous contacting and stage efficiencies; HTU and NTU concepts; design and operation of equipment for distillation, absorption, leaching, liquid-liquid extraction, drying, humidification, dehumidification and adsorption; membrane separations (micro-filtration, ultra-filtration, nano-filtration and reverse osmosis)",
            ]),
            dict(id="cre", name="Chemical Reaction Engineering", short="CRE", weight=12, syllabus=[
                "Theories of reaction rates; kinetics of homogeneous reactions, interpretation of kinetic data, single and multiple reactions in ideal reactors, kinetics of enzyme reactions (Michaelis-Menten and Monod models), non-ideal reactors; residence time distribution, single parameter model; non-isothermal reactors; kinetics of heterogeneous catalytic reactions; diffusion effects in catalysis",
            ]),
            dict(id="ipc", name="Instrumentation & Process Control", short="IPC", weight=10, syllabus=[
                "Measurement of process variables; sensors and transducers; PandID equipment symbols; process modelling and linearization, transfer functions and dynamic responses of various systems, systems with inverse response, process reaction curve, controller modes (P, PI, and PID); control valves; transducer dynamics; analysis of closed loop systems including stability, frequency response, controller tuning, cascade and feed forward control",
            ]),
            dict(id="pd", name="Plant Design & Economics", short="PDE", weight=8, syllabus=[
                "Principles of process economics and cost estimation including depreciation and total annualized cost, cost indices, rate of return, payback period, discounted cash flow, optimization in process design and sizing of chemical engineering equipments such as compressors, heat exchangers, multistage contactors",
            ]),
            dict(id="ct", name="Chemical Technology", short="Chem Tech", weight=8, syllabus=[
                "Inorganic chemical industries (sulfuric acid, phosphoric acid, chlor-alkali industry), fertilizers (ammonia, urea, SSP and TSP); natural products industries (pulp and paper, sugar, oil and fats); petroleum refining and petrochemicals; polymerization industries (polyethylene, polypropylene, PVC and polyester synthetic fibers)",
            ]),
        ],
    ),

    dict(
        id="in", code="IN", name="Instrumentation Engineering", short="Instrumentation",
        subjects=[
            GENERAL_APTITUDE,
            dict(id="maths", name="Engineering Mathematics", short="Math", weight=13,
                 syllabus=ENGG_MATHS_COMMON),
            dict(id="emag", name="Electricity & Magnetism", short="E&M", weight=7, syllabus=[
                "Coulomb's law, electric field intensity, electric flux density, Gauss's law, divergence, electric field and potential due to point, line, plane and spherical charge distributions, effect of dielectric medium, capacitance of simple configurations",
                "Biot-Savart's law, Ampere's law, curl, Faraday's law, Lorentz force, inductance, magnetomotive force, reluctance, magnetic circuits, self and mutual inductance of simple configurations",
            ]),
            dict(id="ecm", name="Electrical Circuits & Machines", short="Circuits", weight=12, syllabus=[
                "Voltage and current sources: independent, dependent, ideal and practical; v-i relationships of resistor, inductor, mutual inductance and capacitor; transient analysis of RLC circuits with DC excitation",
                "Kirchoff's laws, mesh and nodal analysis, superposition, Thevenin, Norton, maximum power transfer and reciprocity theorems",
                "Peak, average and RMS values of AC quantities; apparent, active and reactive powers; phasor analysis, impedance and admittance; series and parallel resonance, locus diagrams, realization of basic filters with R, L and C elements",
                "Transformers: ideal and practical, equivalent circuit, losses, efficiency, regulation and testing; three-phase induction motors, DC servo motors, AC servo motors, stepper motors",
            ]),
            dict(id="ss", name="Signals & Systems", short="S&S", weight=8, syllabus=[
                "Periodic, aperiodic and impulse signals; Laplace, Fourier and z-transforms; transfer function, frequency response of first and second order linear time invariant systems, impulse response of systems; convolution, correlation; discrete time system — impulse response, frequency response, pulse transfer function; DFT and FFT; basics of IIR and FIR filters",
            ]),
            dict(id="cs_ctrl", name="Control Systems", short="Control", weight=10, syllabus=[
                "Feedback principles, signal flow graphs, transient response, steady-state errors, Bode plot, phase and gain margins, Routh and Nyquist criteria, root loci, design of lead, lag and lead-lag compensators, state-space representation of systems; time-delay systems; mechanical, hydraulic and pneumatic system components, synchro pair, servo and step motors, on-off, P, P-I, P-I-D, cascade, feedforward and ratio controllers",
            ]),
            dict(id="ae", name="Analog Electronics", short="Analog", weight=10, syllabus=[
                "Characteristics and applications of diode, Zener diode, BJT and MOSFET; small signal analysis of transistor circuits, feedback amplifiers; characteristics of operational amplifiers, applications of opamps — difference amplifier, adder, subtractor, integrator, differentiator, instrumentation amplifier, precision rectifier, active filters, oscillators, signal generators, voltage controlled oscillators and phase locked loop, sources and effects of noise and interference in electronic circuits",
            ]),
            dict(id="de", name="Digital Electronics", short="Digital", weight=10, syllabus=[
                "Combinational logic circuits, minimization of Boolean functions; IC families — TTL and CMOS; arithmetic circuits, comparators, Schmitt trigger, multi-vibrators, sequential circuits, flip-flops, shift registers, timers and counters; sample-and-hold circuit, multiplexer, analog-to-digital (successive approximation, integrating, flash and sigma-delta) and digital-to-analog converters (weighted R, R-2R ladder and current steering logic)",
                "Characteristics of ADC and DAC (resolution, quantization, significant bits, conversion/settling time); basics of number systems, embedded systems — microprocessor and microcontroller applications, memory and input-output interfacing; basics of data acquisition systems, basics of distributed control systems, basics of computer-based control systems",
            ]),
            dict(id="meas", name="Measurements", short="Meas", weight=10, syllabus=[
                "SI units, systematic and random errors in measurement, expression of uncertainty — accuracy and precision index, propagation of errors; PMMC, MI and dynamometer type instruments; DC potentiometer; bridges for measurement of R, L and C, Q-meter",
                "Measurement of voltage, current and power in single and three phase circuits; AC and DC current probes; true RMS meters, voltage and current scaling, instrument transformers, timer/counter, time, phase and frequency measurements, digital voltmeter, digital multimeter; oscilloscope, shielding and grounding",
            ]),
            dict(id="sii", name="Sensors & Industrial Instrumentation", short="Sensors", weight=12, syllabus=[
                "Resistive, capacitive, inductive, piezoelectric, Hall effect sensors and associated signal conditioning circuits; transducers for industrial instrumentation — displacement (linear and angular), velocity, acceleration, force, torque, vibration, shock, pressure (including low pressure), flow (variable head, variable area, electromagnetic, ultrasonic, turbine and open channel flow meters), temperature (thermocouple, bolometer, RTD, thermistor, pyrometer and semiconductor); liquid level, pH, conductivity and viscosity measurement",
            ]),
            dict(id="coi", name="Communication & Optical Instrumentation", short="Comm", weight=8, syllabus=[
                "Amplitude and frequency modulation and demodulation; Shannon's sampling theorem, pulse code modulation; frequency and time division multiplexing, amplitude, phase, frequency, pulse shift keying for digital modulation; optical sources and detectors — LED, laser, photo-diode, light dependent resistor and their characteristics; interferometer — applications in metrology; basics of fiber optic sensing",
            ]),
        ],
    ),

    dict(
        id="da", code="DA", name="Data Science & Artificial Intelligence", short="DS & AI",
        subjects=[
            GENERAL_APTITUDE,
            dict(id="prob", name="Probability & Statistics", short="Prob", weight=15, syllabus=[
                "Counting (permutations and combinations), probability axioms, sample space, events, independent events, mutually exclusive events, marginal, conditional and joint probability, Bayes theorem",
                "Conditional expectation and variance, mean, median, mode and standard deviation, correlation and covariance",
                "Random variables, discrete random variables and probability mass functions, uniform, Bernoulli, binomial distribution; continuous random variables and probability distribution functions, uniform, exponential, Poisson, normal, standard normal, t-distribution, chi-squared distributions, cumulative distribution function, conditional PDF",
                "Central limit theorem, confidence interval, z-test, t-test, chi-squared test",
            ]),
            dict(id="la", name="Linear Algebra", short="LinAlg", weight=10, syllabus=[
                "Vector space, subspaces, linear dependence and independence of vectors, matrices, projection matrix, orthogonal matrix, idempotent matrix, partition matrix and their properties",
                "Quadratic forms, systems of linear equations and solutions; Gaussian elimination, eigenvalues and eigenvectors, determinant, rank, nullity, projections, LU decomposition, singular value decomposition",
            ]),
            dict(id="calc", name="Calculus & Optimization", short="Calc", weight=8, syllabus=[
                "Functions of a single variable, limit, continuity and differentiability, Taylor series, maxima and minima, optimization involving a single variable",
            ]),
            dict(id="pdsa", name="Programming, Data Structures & Algorithms", short="PDSA", weight=15, syllabus=[
                "Programming in Python, basic data structures — stacks, queues, linked lists, trees, hash tables",
                "Search algorithms — linear search and binary search; basic sorting algorithms — selection sort, bubble sort and insertion sort; divide and conquer — mergesort, quicksort",
                "Introduction to graph theory; basic graph algorithms — traversals and shortest path",
            ]),
            dict(id="dbw", name="Database Management & Warehousing", short="DBMS", weight=10, syllabus=[
                "ER-model, relational model — relational algebra, tuple calculus, SQL, integrity constraints, normal forms, file organization, indexing, data types",
                "Data transformation such as normalization, discretization, sampling, compression; data warehouse modelling — schema for multidimensional data models, concept hierarchies, measures — categorization and computations",
            ]),
            dict(id="ml", name="Machine Learning", short="ML", weight=17, syllabus=[
                "Supervised Learning: regression and classification problems, simple linear regression, multiple linear regression, ridge regression, logistic regression, k-nearest neighbour, naive Bayes classifier, linear discriminant analysis, support vector machine, decision trees, bias-variance trade-off, cross-validation methods (leave-one-out, k-folds), multi-layer perceptron, feed-forward neural network",
                "Unsupervised Learning: clustering algorithms — k-means/k-medoid, hierarchical clustering (top-down, bottom-up), single-linkage, multiple-linkage; dimensionality reduction — principal component analysis",
            ]),
            dict(id="ai", name="Artificial Intelligence", short="AI", weight=10, syllabus=[
                "Search: informed, uninformed, adversarial",
                "Logic: propositional, predicate",
                "Reasoning under uncertainty topics — conditional independence representation, exact inference through variable elimination, and approximate inference through sampling",
            ]),
        ],
    ),
]


# ---------------------------------------------------------------------------
# The remaining papers: selectable, with their section names.
# ---------------------------------------------------------------------------

OUTLINE = [
    ("ae", "AE", "Aerospace Engineering", "Aerospace", [
        "Engineering Mathematics", "Flight Mechanics", "Space Dynamics",
        "Aerodynamics", "Structures", "Propulsion",
    ]),
    ("ag", "AG", "Agricultural Engineering", "Agricultural", [
        "Engineering Mathematics", "Farm Machinery", "Farm Power",
        "Soil and Water Conservation Engineering", "Irrigation and Drainage Engineering",
        "Agricultural Processing Engineering", "Dairy and Food Engineering",
    ]),
    ("ar", "AR", "Architecture and Planning", "Architecture", [
        "Architecture and Design", "Building Materials, Construction and Management",
        "Building and Structures", "Environmental Planning and Design",
        "Urban Design, Landscape and Conservation", "Planning Process",
        "Housing", "Services, Infrastructure and Transportation",
    ]),
    ("bm", "BM", "Biomedical Engineering", "Biomedical", [
        "Engineering Mathematics", "Electrical Circuits", "Signals and Systems",
        "Analog and Digital Electronics", "Measurements and Control Systems",
        "Sensors and Bioinstrumentation", "Human Anatomy and Physiology",
        "Medical Imaging Systems", "Biomechanics", "Biomaterials",
    ]),
    ("bt", "BT", "Biotechnology", "Biotech", [
        "Engineering Mathematics", "General Biotechnology",
        "Recombinant DNA Technology", "Plant and Animal Biotechnology",
        "Bioprocess Engineering and Process Biotechnology",
    ]),
    ("cy", "CY", "Chemistry", "Chemistry", [
        "Physical Chemistry", "Inorganic Chemistry", "Organic Chemistry",
    ]),
    ("es", "ES", "Environmental Science & Engineering", "Environmental", [
        "Mathematics Foundation", "Environmental Chemistry", "Environmental Microbiology",
        "Water Resources and Environmental Hydraulics", "Water and Wastewater Treatment and Management",
        "Air and Noise Pollution", "Solid and Hazardous Waste Management",
        "Global and Regional Environmental Issues", "Environmental Management and Sustainable Development",
    ]),
    ("ey", "EY", "Ecology and Evolution", "Ecology", [
        "Ecology", "Evolution", "Mathematics and Quantitative Ecology",
        "Behavioural Ecology", "Applied Ecology and Evolution",
    ]),
    ("ge", "GE", "Geomatics Engineering", "Geomatics", [
        "Mathematics", "Surveying Measurements and Adjustments",
        "Coordinate Systems and Datums", "Photogrammetry", "Remote Sensing",
        "GNSS", "GIS and Cartography", "Spatial Data Analysis",
    ]),
    ("gg", "GG", "Geology and Geophysics", "Geology", [
        "Common Section: Earth and Planetary System, Weathering and Soil Formation",
        "Geology: Mineralogy, Structural Geology, Palaeontology, Stratigraphy, Petrology, Economic Geology",
        "Geophysics: Gravity, Magnetic, Electrical, Seismic, Well Logging, Radiometric methods",
    ]),
    ("ma", "MA", "Mathematics", "Mathematics", [
        "Calculus", "Linear Algebra", "Real Analysis", "Complex Analysis",
        "Ordinary Differential Equations", "Algebra", "Functional Analysis",
        "Numerical Analysis", "Partial Differential Equations", "Topology",
        "Linear Programming",
    ]),
    ("mn", "MN", "Mining Engineering", "Mining", [
        "Engineering Mathematics", "Mine Development and Surveying",
        "Geomechanics and Ground Control", "Mining Methods and Machinery",
        "Surface Environment, Mine Ventilation and Underground Hazards",
        "Mineral Economics, Mine Planning, Systems Engineering",
    ]),
    ("mt", "MT", "Metallurgical Engineering", "Metallurgy", [
        "Engineering Mathematics", "Thermodynamics and Rate Processes",
        "Extractive Metallurgy", "Physical Metallurgy",
        "Mechanical Metallurgy", "Manufacturing Processes",
    ]),
    ("nm", "NM", "Naval Architecture & Marine Engineering", "Naval", [
        "Engineering Mathematics", "Applied Mechanics and Structures",
        "Fluid Mechanics and Marine Hydrodynamics", "Thermodynamics and Marine Engineering",
        "Ship Design, Resistance and Propulsion", "Ship Manoeuvring and Motions",
    ]),
    ("pe", "PE", "Petroleum Engineering", "Petroleum", [
        "Linear Algebra and Calculus", "Petroleum Exploration",
        "Oil and Gas Well Drilling Technology", "Reservoir Engineering",
        "Petroleum Production Operations", "Offshore Drilling and Production Practices",
        "Petroleum Formation Evaluation", "Oil and Gas Well Testing",
        "Health Safety and Environment in Petroleum Industry",
        "Enhanced Oil Recovery Techniques", "Latest trends in Petroleum Engineering",
    ]),
    ("ph", "PH", "Physics", "Physics", [
        "Mathematical Physics", "Classical Mechanics", "Electromagnetic Theory",
        "Quantum Mechanics", "Thermodynamics and Statistical Physics",
        "Atomic and Molecular Physics", "Solid State Physics and Electronics",
        "Nuclear and Particle Physics",
    ]),
    ("pi", "PI", "Production & Industrial Engineering", "Production", [
        "Engineering Mathematics", "General Engineering",
        "Manufacturing Processes I", "Manufacturing Processes II",
        "Quality and Reliability", "Industrial Engineering",
        "Operations Research and Operations Management",
    ]),
    ("st", "ST", "Statistics", "Statistics", [
        "Calculus", "Matrix Theory", "Probability", "Stochastic Processes",
        "Estimation", "Testing of Hypotheses", "Non-parametric Statistics",
        "Multivariate Analysis", "Regression Analysis", "Design of Experiments",
        "Sampling",
    ]),
    ("tf", "TF", "Textile Engineering & Fibre Science", "Textile", [
        "Engineering Mathematics", "Textile Fibres", "Yarn Manufacture, Structure and Properties",
        "Fabric Manufacture, Structure and Properties", "Textile Testing",
        "Chemical Processing",
    ]),
    ("xe", "XE", "Engineering Sciences", "Engg Sciences", [
        "Engineering Mathematics (compulsory)", "Fluid Mechanics", "Materials Science",
        "Solid Mechanics", "Thermodynamics", "Polymer Science and Engineering",
        "Food Technology", "Atmospheric and Ocean Sciences", "Energy Science",
    ]),
    ("xh", "XH", "Humanities & Social Sciences", "Humanities", [
        "Reasoning and Comprehension (compulsory)", "Economics", "English",
        "Linguistics", "Philosophy", "Psychology", "Sociology",
    ]),
    ("xl", "XL", "Life Sciences", "Life Sciences", [
        "Chemistry (compulsory)", "Biochemistry", "Botany", "Microbiology",
        "Zoology", "Food Technology",
    ]),
]


def all_branches() -> list[dict]:
    """Every GATE 2026 paper, detailed ones first, then the rest alphabetically."""
    out: list[dict] = []

    for branch in DETAILED:
        out.append(dict(branch, detail="full"))

    for bid, code, name, short, sections in sorted(OUTLINE, key=lambda b: b[2]):
        subjects = [GENERAL_APTITUDE]
        for i, section in enumerate(sections):
            subjects.append(dict(
                id="%s_s%d" % (bid, i),
                name=section,
                short=section[:12],
                # 85 technical marks spread evenly; indicative only.
                weight=max(1, round(85 / len(sections))),
                syllabus=[],
            ))
        out.append(dict(
            id=bid, code=code, name=name, short=short,
            subjects=subjects, detail="outline",
        ))

    return out
