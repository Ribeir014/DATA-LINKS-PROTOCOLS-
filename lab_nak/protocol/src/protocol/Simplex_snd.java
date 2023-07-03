/*
 * Sistemas de Telecomunicacoes 
 *          2022/2023
 */
package protocol;

import simulator.AckFrameIF;
import simulator.DataFrameIF;
import terminal.Simulator;
import simulator.Frame;
import terminal.NetworkLayer;

/**
 * Protocol 2 : Simplex Sender protocol which does not receive frames
 * 
 * @author 62695 (Put here your students' numbers)
 */
public class Simplex_snd extends Base_Protocol implements Callbacks {
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
     * Sequence number of the next data frame
     */
    private int next_frame_to_send;
    
    /**
     * Sending buffer
     */
    private String sending_buffer;
    
    
    public Simplex_snd(Simulator _sim, NetworkLayer _net) {
        super(_sim, _net);      // Calls the constructor of Base_Protocol
        next_frame_to_send = 0;
        // ...
    }
    
    /**
     * Fetches the network layer for the next packet and starts it transmission
     * @return true is started data frame transmission, false otherwise
     */
    private void send_next_data_packet() {
        String packet= net.from_network_layer();
        sending_buffer = packet;
        if (packet != null) {
            Frame frame = Frame.new_Data_Frame(next_frame_to_send /*seq*/, 
                    prev_seq(0) /* ack= the one before 0 */, 
                    net.get_recvbuffsize() /* returns the buffer space available in the network layer */,
                    packet);
            sim.to_physical_layer(frame, false /* do not interrupt an ongoing transmission*/);
            next_frame_to_send = next_seq(next_frame_to_send); 
            // Transmission of next DATA frame occurs after DATA_END event is received
        }
    }
    
    /**
     * Gets the previous packet to transmit again
     * @return true is started data frame transmission, false otherwise
     */
    private void retransmit_data_packet() {
        String packet = sending_buffer;
        if (packet != null) {
            Frame frame = Frame.new_Data_Frame(prev_seq(next_frame_to_send) /*seq*/, 
                    prev_seq(0) /* ack= the one before 0 */, 
                    net.get_recvbuffsize() /* returns the buffer space available in the network layer */,
                    packet);
            sim.to_physical_layer(frame, false /* do not interrupt an ongoing transmission*/);
        }
    }
    
    /**
     * CALLBACK FUNCTION: handle the beginning of the simulation event
     * @param time current simulation time
     */
    @Override
    public void start_simulation(long time) {
        sim.Log("\nSimplex Sender Protocol\n\tOnly send data!\n\n");
        send_next_data_packet();
    }

    /**
     * CALLBACK FUNCTION: handle the end of Data frame transmission, start timer
     * @param time current simulation time
     * @param seq  sequence number of the Data frame transmitted
     */
    @Override
    public void handle_Data_end(long time, int seq) {
        sim.start_data_timer(seq);
        // Do nothing
    }
    
    /**
     * CALLBACK FUNCTION: handle the data timer event; retransmit failed frames
     * @param time current simulation time
     * @param key   timer key (sequence number)  
     */
    @Override
    public void handle_Data_Timer(long time, int key) {
        retransmit_data_packet();
    }
    
    /**
     * CALLBACK FUNCTION: handle the ack timer event; send ACK frame
     * @param time current simulation time
     */
    @Override
    public void handle_ack_Timer(long time) {
        sim.Log(time + " ACK Timeout - ignored\n");
    }

    /**
     * CALLBACK FUNCTION: handle the reception of a frame from the physical layer
     * @param time current simulation time
     * @param frame frame received
     */
    @Override
    public void from_physical_layer(long time, Frame frame) {
        if (frame.kind() == Frame.ACK_FRAME) {
            AckFrameIF aframe= frame;  // Auxiliary variable to access the Ack frame fields.
            if (aframe.ack() == prev_seq(next_frame_to_send)) {
                sim.cancel_data_timer(prev_seq(next_frame_to_send));
                send_next_data_packet();
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
