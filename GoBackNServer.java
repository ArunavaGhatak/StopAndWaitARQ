// @author Arunava Ghatak

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
 
public class GoBackNServer {

	private static final double LOSS_RATE = 0.3;
	private static final int WINSIZE = 3;
	private static final int DATASIZE = 512;
 
	public static void main(String[] args) {

		DatagramSocket ss = null;
		FileInputStream fis = null;
		DatagramPacket rp, sp;
		byte[] rd, sd;
		Random random = new Random();

		InetAddress ip;
		int port;
 		while(true) {
		try {
			ss = new DatagramSocket(Integer.parseInt(args[0]));
			System.out.println("\n\nServer is up....");

			byte[] RDT = new byte[] { 0x52, 0x44, 0x54, 0x20 };
    			byte[] END = new byte[] { 0x20, 0x45, 0x4e, 0x44 };
    			byte[] CRLF = new byte[] { 0x20, 0x0d, 0x0a };
			

			byte firstConsignment=0;
    			byte[] seqNo = new byte[] { 0x00, 0x20 };
			String receivedStr;
			LinkedList<Byte[]> window = new LinkedList<Byte[]>();
			

			// get client's request from DatagramPacket
			while(true){
				rd=new byte[100];
				rp = new DatagramPacket(rd,rd.length);
				ss.receive(rp);
				receivedStr = new String(rp.getData());
				if(Pattern.compile("REQUEST.*\r\n.*", Pattern.DOTALL).matcher(receivedStr).matches()) break;
			}
			String fileName = receivedStr.split("\r\n")[0].substring(7);
			fis = new FileInputStream(fileName);
			System.out.println("Client request for " + fileName + " received.\n");
			rd = null;
	 		

			// Get client's port no. and IP
			ip = rp.getAddress(); 
			port = rp.getPort();
			System.out.println("Client IP Address = " + ip);
			System.out.println("Client port = " + port + "\n");
			
			ss.setSoTimeout(30);
			boolean resend = false;
			boolean allRead = false;
			int consignmentsToBeRead = WINSIZE;
			
			while(true){

				//Resend whole window if no acknowledgement is received before timeout
				if(resend == true){
					for (Byte i[] : window) {
						sd = getBytes(i);
						sp=new DatagramPacket(sd,sd.length,ip,port);
						if (random.nextDouble() < LOSS_RATE) {
            						System.out.println("Forgot to resend CONSIGNMENT " + sd[4]);
         					}
						else {
							ss.send(sp);
							System.out.println("Resent CONSIGNMENT " + sd[4]);
						}
					}
				}

				// Otherwise read and send new consignments
				else if(allRead == false)
				while(allRead == false && consignmentsToBeRead-- != 0) {
					byte[] data = new byte[DATASIZE];
					int n = fis.read(data);
					if(n == DATASIZE) sd = concatenateByteArrays(RDT, seqNo, data, CRLF, new byte[] { 0x20, 0x20, 0x20, 0x20 });
					else {
						if(n == -1) n = 0;
						byte[] spaces = new byte[DATASIZE - n];
						Arrays.fill(spaces, (byte)0x20);
						sd = concatenateByteArrays(RDT, seqNo, Arrays.copyOf(data, n), END, CRLF, spaces);
					}
					sp=new DatagramPacket(sd,sd.length,ip,port);
					if (random.nextDouble() < LOSS_RATE) {
            					System.out.println("Forgot CONSIGNMENT " + sd[4]);
         				}
					else {
						ss.send(sp);
						System.out.println("Sent CONSIGNMENT " + sd[4]);
					}
					window.add(getBytes(sd));
					seqNo[0]++;
					if(n != DATASIZE) {allRead = true; break;}
				}


				// Wait to receive acknowledgement
				try{
				byte nextConsignment;
				while(true) {
					rd = new byte[8];
					rp = new DatagramPacket(rd,rd.length);
					ss.receive(rp);
					receivedStr = new String(rp.getData());
					if(!Pattern.compile("ACK . \r\n", Pattern.DOTALL).matcher(receivedStr).matches()) continue;
					nextConsignment = rd[4];
					if(nextConsignment != 0 && 
						(nextConsignment <= firstConsignment || nextConsignment > firstConsignment + WINSIZE)) continue;
					resend = false;
					System.out.println("Received ACK " + nextConsignment + "\n");
					break;
				}
				if(nextConsignment == 0) break;
				consignmentsToBeRead = 0;
				while(firstConsignment != nextConsignment) {
					window.remove();
					++firstConsignment;
					++consignmentsToBeRead;
				}
				}
				// Handle exception if no acknowledgement received
				catch(SocketTimeoutException e) {
					System.out.println("Timeout. No acknowledgement received.\n");
					resend = true;
				}
	 
			} // while true

		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		} finally {
			try {
				if (fis != null)
					fis.close();
				if (ss != null)
					ss.close();
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
			}
		}
		}
	}

	public static byte[] concatenateByteArrays(byte[] a, byte[] b, byte[] c, byte[] d, byte[] e) {
        	byte[] result = new byte[a.length + b.length + c.length + d.length + e.length]; 
        	System.arraycopy(a, 0, result, 0, a.length); 
        	System.arraycopy(b, 0, result, a.length, b.length);
        	System.arraycopy(c, 0, result, a.length+b.length, c.length);
        	System.arraycopy(d, 0, result, a.length+b.length+c.length, d.length);
        	System.arraycopy(e, 0, result, a.length+b.length+c.length+d.length, e.length);
        	return result;
    }
    public static byte[] concatenateByteArrays(byte[] a, byte[] b, byte[] c, byte[] d, byte[] e, byte[] f) {
        	byte[] result = new byte[a.length + b.length + c.length + d.length + e.length + f.length]; 
        	System.arraycopy(a, 0, result, 0, a.length); 
        	System.arraycopy(b, 0, result, a.length, b.length);
        	System.arraycopy(c, 0, result, a.length+b.length, c.length);
        	System.arraycopy(d, 0, result, a.length+b.length+c.length, d.length);
        	System.arraycopy(e, 0, result, a.length+b.length+c.length+d.length, e.length);
        	System.arraycopy(f, 0, result, a.length+b.length+c.length+d.length+e.length, f.length);
        	return result;
    }
    public static Byte[] getBytes(byte[] a) {
        Byte r[] = new Byte[a.length];
	for(int i = 0; i < a.length; ++i)
		r[i] = new Byte(a[i]);
        return r;
    }
    public static byte[] getBytes(Byte[] a) {
        byte r[] = new byte[a.length];
	    for(int i = 0; i < a.length; ++i)
		    r[i] = a[i].byteValue();
        return r;
    }
}

