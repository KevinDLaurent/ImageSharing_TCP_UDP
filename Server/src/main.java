import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class main {
	
	
	private static final int MIN_PORT = 5001;
	private static final int MAX_PORT = 5050;
	private static ArrayList<Integer> used_ports = new ArrayList<Integer>();
	private static Scanner scanner = new Scanner(System.in);
	
	/**
	 * @param protocol Serveur pour lequel on choisit le port
	 * @return un port valide
	 * s'assure d'obtenir un port non utilisé entre 5001 et 5050
	 */
	private static int getPort(String protocol){
		
		int port = -1;
		String input = null;
		
		while(port<MIN_PORT || port>MAX_PORT || used_ports.contains(port)){
			System.out.println("Enter a port between "+MIN_PORT+" and "+MAX_PORT+" for the "+protocol+" server.");
			if(scanner.hasNextLine()){				
				input = scanner.nextLine();
			}
			input = input.trim();
			
			try{
				port = Integer.parseInt(input);
			}
			catch(NumberFormatException nfe){
				System.out.println("Error! Input is not an integer.");
			}
			finally{
				if(used_ports.contains(port)){
					System.out.println("The port "+port+" is already in use");
				}
			}
			
		}
		
		used_ports.add(port);
		return port;
	}

	
	/**
	 * @param args non utilisé
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * démarre les 2 serveurs dans leur thread
	 */
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		System.out.println("SERVER");
		int port_TCP = getPort("TCP");
		int port_UDP = getPort("UDP");
		scanner.close();
		
		Thread tcp = new Thread(new TCPServer(port_TCP));
		Thread udp = new Thread(new UDPServer(port_UDP));
		//TCPServer tcp = new TCPServer(port_TCP);
		//UDPServer udp = new UDPServer(port_TCP);
		tcp.start();
		udp.start();
		while(true) {
			//busy wait
			
		}
		//System.out.println("End");
	}

}
