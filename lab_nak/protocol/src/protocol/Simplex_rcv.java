/*
 * Sistemas de Telecomunicacoes 
 *          2022/2023
 */
package protocol;

import simulator.DataFrameIF;
import terminal.Simulator;
import simulator.Frame;
import terminal.NetworkLayer;
import simulator.AckFrameIF;

/**
 * Protocol 2 : Simplex Receiver protocol which does not transmit frames
 * 
 * @author 62695 (Put here your students' numbers)
 */
public class Simplex_rcv extends Base_Protocol implements Callbacks {
    /* Variables */
    
    /**
     * Reference to the simulator (Terminal), to get the configuration and send commands
     */
    //final Simulator sim;  -  Inherited from Base_Protocol
    
    /**
     * Reference to the network layer, to send a receive packets
     */
    //final NetworkLayer net;    -  Inherited from Base_Protocol
    
    /**
     * Expected sequence number of the next data frame received
     */
    private int frame_expected;
    
    
    public Simplex_rcv(Simulator _sim, NetworkLayer _net) {
        super(_sim, _net);      // Calls the constructor of Base_Protocol
        frame_expected = 0;
    }

    /**
     * Create and send ack
     */
    private void send_ack() {
        //create ack
        Frame aframe = Frame.new_Ack_Frame(prev_seq(frame_expected), net.get_recvbuffsize());
        //send ack
        sim.to_physical_layer(aframe, false);
    }
    
    /**
     * CALLBACK FUNCTION: handle the beginning of the simulation event
     * @param time current simulation time
     */
    @Override
    public void start_simulation(long time) {
        sim.Log("\nSimplex Receiver Protocol\n\tOnly receive data!\n\n");
    }
    
    /**
     * CALLBACK FUNCTION: handle the reception of a frame from the physical layer
     * @param time current simulation time
     * @param frame frame received
     */
    @Override
    public void from_physical_layer(long time, Frame frame) {
        if (frame.kind() == Frame.DATA_FRAME) {     // Check the frame kind
            DataFrameIF dframe= frame;  // Auxiliary variable to access the Data frame fields.
            if (dframe.seq() == frame_expected) {    // Check the sequence number
                // Send the frame to the network layer
                if (net.to_network_layer(dframe.info())) {
                    frame_expected = next_seq(frame_expected);
                    send_ack();
                }
            }
            else if (dframe.seq() == prev_seq(frame_expected)) {
                send_ack();
            }            
        }
    }

    /**
     * CALLBACK FUNCTION: handle the end of the simulation
     * @param time current simulation time
     */
    @Override
    public void end_simulation(long time) {
        sim.Log("Stopping simulation\n");
    }
    
    

    
}
