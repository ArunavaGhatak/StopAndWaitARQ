// @author Arunava Ghatak

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
 
public class StopAndWaitClient {

	private static final double LOSS_RATE = 0.3;
	private static final int DATASIZE = 512;
 
	public static void main(String[] args) {
	 
	    DatagramSocket cs = null;
	    FileOutputStream fos = null;
	    Random random = new Random();


		try {
			cs = new DatagramSocket();
			fos = new FileOutputStream(args[3]);

			// Send request to access file to Server
			byte[] rd, sd;
			String request = "REQUEST" + args[2] + "\r\n";
			byte lastSeqNo = -1;
			DatagramPacket sp,rp;
			sd=request.getBytes();
			sp=new DatagramPacket(sd,sd.length, InetAddress.getByName(args[0]),
  									Integer.parseInt(args[1]));
			cs.send(sp);
			System.out.println("Request to access file sent.\n");
			
			boolean end = false;
			
			while(true)
			{   	
				//receive next message
			        rd=new byte[DATASIZE + 13];
			        rp=new DatagramPacket(rd,rd.length); 
			        try{cs.receive(rp);}
				catch(SocketTimeoutException e) {break;}	


				//Extract the payload and write it to the output file if it is the expected consignment
				byte[] data = rp.getData();
				String dataStr=new String(data);
				if(!Pattern.compile("RDT . .+ \r\n[ ]*", Pattern.DOTALL).matcher(dataStr).matches()) continue;
				dataStr = dataStr.split("\r\n[ ]*$")[0];
				byte sentSeqNo = rd[4];
				if(sentSeqNo != lastSeqNo + 1) {
					System.out.println("Received and discarded CONSIGNMENT " + sentSeqNo);
					if(sentSeqNo != lastSeqNo) continue;
				}
				if(sentSeqNo == lastSeqNo + 1) {
					if (dataStr.substring(dataStr.length() - 4).equals("END ")) { // last consignment
						data = extractBytes(data, 6, dataStr.length() - 5); // extract payload
						end = true;
					}
					else data = extractBytes(data, 6, dataStr.length() - 1); // extract payload
					fos.write(data);
					System.out.println("Received CONSIGNMENT " + sentSeqNo);
				}
				

				// send Acknowledgement
				lastSeqNo = sentSeqNo;
			        sd=new byte[] {0x41, 0x43, 0x4b, 0x20, 0x00, 0x20, 0x0d, 0x0a}; //ACK (0X00) \r\a
				if(end) sd[4] = 0; else sd[4] = (byte)(lastSeqNo + 1);	 
			        sp=new DatagramPacket(sd,sd.length, InetAddress.getByName(args[0]),
  									Integer.parseInt(args[1]));	 
				if (random.nextDouble() < LOSS_RATE) {
            				System.out.println("Forgot ACK " + sd[4] + "\n");
         			}
				else {
					cs.send(sp);	
					System.out.println("Sent ACK " + sd[4] + "\n");
				}
				if(end) cs.setSoTimeout(500);
				

			}

		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		} finally {

			try {
				if (fos != null)
					fos.close();
				if (cs != null)
					cs.close();
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
			}
		}
	}

	public static byte[] extractBytes(byte[] a, int i, int j) {
	byte b[] = new byte[j - i];
	for(int k = i; k < j; ++k)
		b[k - i] = a[k];
	return b;
	}
 
}
