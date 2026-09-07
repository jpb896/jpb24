package com.jpb.jpb24x.providers

// This interface defines the contract. It belongs in your core library layer.
interface SocHardwareProvider {
    /** Returns a list of possible SoC ID strings gathered from the system hardware. */
    fun getPossibleSocIds(): List<String>
}