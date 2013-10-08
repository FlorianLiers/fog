package de.tuilmenau.ics.fog.emulator;

import de.tuilmenau.ics.fog.emulator.packets.Packet;

public interface Port
{
	public int getPortNumber();
	public void handlePacket(Packet packet);
}
