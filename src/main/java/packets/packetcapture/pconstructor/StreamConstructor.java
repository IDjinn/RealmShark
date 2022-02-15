package packets.packetcapture.pconstructor;

import jpcap.packet.TCPPacket;
import util.Util;

import java.util.HashMap;

/**
 * Stream constructor ordering TCP packets in sequence. Packets are sent to the rotmg
 * constructor if they are in sequence.
 */
public class StreamConstructor implements PConstructor {

    PReset packetReset;
    PConstructor packetConstructor;

    /**
     * Constructor of StreamConstructor which needs a reset class to reset if reset
     * packet is retrieved and a constructor class to send ordered packets to.
     *
     * @param pr Reset class if a reset packet is retrieved.
     * @param pc Constructor class to send ordered packets to.
     */
    public StreamConstructor(PReset pr, PConstructor pc) {
        packetReset = pr;
        packetConstructor = pc;
    }

    HashMap<Integer, TCPPacket> packetMap = new HashMap();
    public int ident;

    /**
     * Build method for ordering packets according to index used by TCP.
     *
     * @param packet TCP packets needing to be ordered.
     */
    @Override
    public void build(TCPPacket packet) {
        if (packet.ident == 0) {
            if (packet.data.length != 0) {
                throw new IllegalStateException();
            }
            reset();
            return;
        }
        if (ident == 0) {
            ident = packet.ident;
        }
        packetMap.put(packet.ident, packet);
        while (packetMap.containsKey(ident)) {
            TCPPacket packetSeqed = packetMap.remove(ident);
            ident++;
            if (Util.firstNonLargePacket) { // start listening after a non-max packet
                if(packet.data.length < 1460) Util.firstNonLargePacket = false;
                continue;
            }
            packetConstructor.build(packetSeqed);
        }
    }

    /**
     * Reset method if a reset packet is retrieved.
     */
    public void reset() {
        packetReset.reset();
        packetMap.clear();
        ident = 0;
    }
}