/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cidarlab.raven.communication;

/**
 *
 * @author evanappleton
 */
public class AuthenticationException 
	extends Exception {
	
	private static final long serialVersionUID = 7476470055381392172L;

	public AuthenticationException(String exp) {
		super(exp);
	}

}
