# opencl-satsolver

This is a Brute force sat solver, written in Java, and OpenCL (using the JOCL bindings). It basically parses the input expression, and tries every single variable configuration.

There is a test app included, that can be used to try the libs features.

Features:
  * Supports expressions up to 62 boolean variables
  * AND, OR, NEGATE operators
  * Can load .CNF files

Note that above 24-26 variables calculation times can be very-very long.
