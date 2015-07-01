/*
 * Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>
 *
 * This code is made freely available under the MIT license: http://www.opensource.org/licenses/mit-license.php
 */

package edu.fiu.mpact.TrainingReuProject;

import java.math.*;
import java.io.*;

public class PublicKey implements Serializable {

	// k1 is the security parameter. It is the number of bits in n.
	public int k1;

	// n=pq is a product of two large primes (such N is known as RSA modulous.
	public BigInteger n, modulous;

	private static final long serialVersionUID = -4979802656002515205L;

	private void readObject(ObjectInputStream aInputStream)
			throws ClassNotFoundException, IOException {
		// always perform the default de-serialization first
		aInputStream.defaultReadObject();
	}

	private void writeObject(ObjectOutputStream aOutputStream)
			throws IOException {
		// perform the default serialization for all non-transient, non-static
		// fields
		aOutputStream.defaultWriteObject();
	}

	public String toString() {
		return "k1 = " + k1 + ", n = " + n + ", modulous = " + modulous;
	}
}