/*
 * Sistemas de Telecomunicacoes 
 *          2022/2023
 */
package protocol;

import java.util.Arrays;
import simulator.AckFrameIF;
import simulator.DataFrameIF;
import terminal.Simulator;
import simulator.Frame;
import simulator.NakFrameIF;
import terminal.NetworkLayer;
import terminal.Terminal;

/**
 * Protocol 5 : Go-back-N protocol with flow control (buffer size)
 *
 * @author 62695 (Put here your students' numbers)
 */
public class GoBackN_FlowC extends Base_Protocol implements Callbacks {
    /* Variables */
    /**
     * Reference to the simulator (Terminal), to get the configuration and send
     * commands
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
     * Expected sequence number of the next data frame received
     */
    private int frame_expected;
    /**
     * Buffer das tramas
     */
    private String[] window_buffer;
    /** 
     * Size of the sliding window
     */
    private int window_size;
    /**
     * se esta a retransmitir
     */
    private boolean is_retrans;
    /**
     * base do buffer (numero do pacote que esta na base)
     */
    private int base;
    /**
     * numero do pacote que tem de ser retransmitido
     */
    private int count_retran;
    /**
     * numero do ultimo pacote do buffer
     */
    private int last;
    
    public GoBackN_FlowC(Simulator _sim, NetworkLayer _net) {
        super(_sim, _net);      // Calls the constructor of Base_Protocol
        // Initialize here all variables
        next_frame_to_send = 0;
        frame_expected = 0;
        window_buffer = null;
        window_size = 0;
        is_retrans = false;
        base = 0;
        last = 0;
        count_retran = 0;
    }
    
    /**
     * Fetches the network layer for the next packet and starts it transmission
     * @return true is started data frame transmission, false otherwise
     */
    private void send_next_data_packet() {
        if (!is_window_full()) {
            String packet= net.from_network_layer();
            if (packet != null) {
                if (sim.isactive_ack_timer())
                    sim.cancel_ack_timer();
                //guardo o pacote no buffer
                window_buffer[next_frame_to_send] = packet;
                Frame frame = Frame.new_Data_Frame(next_frame_to_send, prev_seq(frame_expected) , net.get_recvbuffsize(), packet);
                sim.to_physical_layer(frame, false /* do not interrupt an ongoing transmission*/);
                last = next_frame_to_send;
                next_frame_to_send = next_seq(next_frame_to_send);       
            }
            else {
                sim.Log("nao ha mais pacotes na rede\n");
            }
        }
        else {
            sim.Log("a janela esta cheia\n");
        }
    }
    
    /**
     * Gets the previous packet to transmit again
     * @return true is started data frame transmission, false otherwise
     */
    private void retransmit_data_packets() {
        // faz com que salte o send_next_packet no handle_data_end para poder retransmitir os pacotes do buffer
        is_retrans = true;
        
        if (count_retran != next_seq(last)) {
            String packet = window_buffer[count_retran];
            if (packet != null) {
                Frame frame = Frame.new_Data_Frame(count_retran, prev_seq(frame_expected) , net.get_recvbuffsize(), packet);
                sim.to_physical_layer(frame, false /* do not interrupt an ongoing transmission*/);
            }
            count_retran = next_seq(count_retran);
        }
        else
            is_retrans = false;
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
     * Apaga o que está para trás do pacote de parametro até a base, incluido o pacote de parametro,  no buffer
     */
    private void clean_buffer(int seq) {
        while (seq != prev_seq(base)) {
            window_buffer[seq] = null;
            seq = prev_seq(seq);
        }
    }
    
    /**
     * Devolve o index do buffer com o valor da sequencia do pacote num_frame
     */
    private int get_index_buffer(int num_packet) {
        return num_packet % (sim.get_max_sequence()+1);
    }
    
    /**
     * Devolve true se a window estiver cheia
     * @return 
     */
    private boolean is_window_full() {
        int index = base;
        int final_index = get_index_buffer(base + window_size - 1);
       
        while (index != final_index) {
            if (window_buffer[index] == null) 
                return false;
            index = next_seq(index);    
        }
        if (index == final_index && window_buffer[index] == null)
            return false;
        return true;
    }
    
    /**
     *  Cria e envia o nak
     * @param seq 
     */
    private void send_nak(int seq) {
        Frame nframe = Frame.new_Nak_Frame(seq, net.get_recvbuffsize());
        sim.to_physical_layer(nframe, false);
    }
    
    /*
    Muda o valor da base e do last consoante o buffer que recebe
    */
    private void change_buffer_size(int bufsize) {
        if (bufsize == 0) {
            sim.start_data_timer(base);
        }
        else if (bufsize < sim.get_send_window()) {
            window_size = bufsize;
            last = get_index_buffer(base+window_size-1);
        }  
    }
    
    /**
     * CALLBACK FUNCTION: handle the beginning of the simulation event
     *
     * @param time current simulation time
     */
    @Override
    public void start_simulation(long time) {
        sim.Log("\nGo-Back-N FlowControl\n\n");
        window_size = sim.get_send_window();
        window_buffer = new String[sim.get_max_sequence()+1];
        send_next_data_packet();
    }
    
    /**
     * CALLBACK FUNCTION: handle the end of Data frame transmission, start timer
     * and send next until reaching the end of the sending window.
     *
     * @param time current simulation time
     * @param seq sequence number of the Data frame transmitted
     */
    @Override
    public void handle_Data_end(long time, int seq) {
        //ativa o temporizador se ainda nao estiver ativado
        sim.Log(Arrays.toString(window_buffer)+"  hi\n");
        if (!sim.isactive_data_timer(base)) {
            sim.start_data_timer(seq);
        }
        if (!is_retrans){ 
            sim.Log("nao retransmite\n");
            send_next_data_packet(); 
        }
        else {
            sim.Log("retransmite\n"); 
            retransmit_data_packets();
        }
    }

    /**
     * CALLBACK FUNCTION: handle the timer event; retransmit failed frames.
     *
     * @param time current simulation time
     * @param key  timer key (sequence number)
     */
    @Override
    public void handle_Data_Timer(long time, int key) {
        sim.Log(Arrays.toString(window_buffer)+"   bye\n");
        count_retran = base;
        retransmit_data_packets(); 
    }

    /**
     * CALLBACK FUNCTION: handle the ack timer event; send ACK frame
     *
     * @param time current simulation time
     */
    @Override
    public void handle_ack_Timer(long time) {
        send_ack();
    }

    /**
     * CALLBACK FUNCTION: handle the reception of a frame from the physical
     * layer
     *
     * @param time current simulation time
     * @param frame frame received
     */
    @Override
    public void from_physical_layer(long time, Frame frame) {
        if (frame.kind() == Frame.DATA_FRAME) {     // Check the frame kind
            DataFrameIF dframe= frame;  // Auxiliary variable to access the Data frame fields.
            
            if (dframe.seq() == frame_expected) {                                                                
                frame_expected = next_seq(frame_expected);
                net.to_network_layer(dframe.info());      
            }
            
            if (between(base, dframe.ack(),next_seq(last))) {
                sim.cancel_data_timer(base);
                clean_buffer(dframe.ack());
                base = next_seq(dframe.ack());
                change_buffer_size(dframe.rcvbufsize());
                if (dframe.rcvbufsize() != 0) {
                    if (dframe.ack() == prev_seq(next_frame_to_send)) {
                        sim.start_ack_timer();
                        send_next_data_packet();
                    }
                    else {
                        sim.start_data_timer(base);
                        if (!is_window_full()) {
                            if (!is_retrans)  {
                                sim.start_ack_timer();
                                send_next_data_packet();  
                            }
                        }
                        else {
                            send_ack();
                        }
                    }
                }
                else 
                    sim.start_data_timer(base);
                
                
            }       
            else {
               change_buffer_size(dframe.rcvbufsize());
               send_ack();
            }
                
                
        }

        else if (frame.kind() == Frame.ACK_FRAME) {
            AckFrameIF aframe = frame;  // Auxiliary variable to access the Ack frame fields.
            
            if (between(base,aframe.ack(),next_seq(last))) {
                sim.cancel_data_timer(base);
                sim.Log(Arrays.toString(window_buffer)+"\n");
                clean_buffer(aframe.ack());
                sim.Log(Arrays.toString(window_buffer)+"\n");
                base = next_seq(aframe.ack());  
                change_buffer_size(aframe.rcvbufsize());
                
                if (aframe.rcvbufsize() != 0) {
                    if (aframe.ack() == prev_seq(next_frame_to_send)) {
                        sim.Log("recebi ultimo pacote posso mandar o proximo\n");
                        send_next_data_packet();
                    }
                    // se nao for comecamos o timer do pacote que estamos a espera de receber e se tivermos espaco no buffer mandamos outro pacote
                    else {
                        sim.start_data_timer(base);
                        if (!is_window_full()) {
                            sim.Log("window nao esta cheia\n");
                            if (!is_retrans)  {
                                sim.Log("posso mandar o proximo pacote\n");
                                send_next_data_packet();
                            }    
                        }
                        else {
                            sim.Log("window esta cheia\n");
                        }
                    }
                }
                else {
                    sim.start_data_timer(base);
                }
                    
            }
        }
    }

    /**
     * CALLBACK FUNCTION: handle the end of the simulation
     *
     * @param time current simulation time
     */
    @Override
    public void end_simulation(long time) {
        sim.Log("Stopping simulation\n");
    }
}