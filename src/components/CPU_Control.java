package components;

// This class control all the components' object and can decode the instruction and execute them

import java.io.*;
import conversion.*;

public class CPU_Control{
	// all the components including Memory
	public ProgramCounter PC = new ProgramCounter();
	public General_Purpose_Registers GPRs = new General_Purpose_Registers();
	public Instruction_Register IR = new Instruction_Register();
	public Index_Registers IXR = new Index_Registers();
	public Memory_Address_Register MAR = new Memory_Address_Register();
	public Memory_Buffer_Register MBR = new Memory_Buffer_Register();
	public Machine_Fault_Register MFR = new Machine_Fault_Register();
	public Memory Mem = new Memory();
	// machine fault status 
	private int mfindex = 0;
	// halt or not
	public int halt = 0;

	public CPU_Control(){
	}
// This sets the initial components of the machine (initial or restart)
	public void initial(){
		PC = new ProgramCounter(0);
		GPRs = new General_Purpose_Registers();
		IR = new Instruction_Register();
		IXR = new Index_Registers(0, 100, 1000);
		MAR = new Memory_Address_Register();
		MBR = new Memory_Buffer_Register();
		MFR.resetMFR();
		Mem = new Memory(2048,7);
		Mem.CPUwrite(1, 6);
		Mem.CPUwrite(6, 0b0000000000000000);
		
		//reset halt
		halt = 0; 
		
		// read IPX.txt and load it to the memory
		try {
			String pathname = "./IPL.txt";
			File IPL = new File(pathname);
			InputStreamReader reader = new InputStreamReader(new FileInputStream(IPL));
			BufferedReader br = new BufferedReader(reader);
			String line = "";
			while (line != null) {
				line  = br.readLine();
				if (line == null) break;
				String[] loadtoMem = line.split(" ");
				mfindex = Mem.writeMem(ConvertHexToDec.convertHexToDec(loadtoMem[0])+7, ConvertHexToDec.convertHexToDec(loadtoMem[1]));
				checkaddress();
			}
			br.close();
		}	catch (Exception e) {
			e.printStackTrace();
		}
	}
	
// This function is for post-Project #1 to run a single cycle of our machine simulator.
	public void runsinglestep(){
		// if the machine halts, the CPU will not work any more
		if (halt == 1) return;
		// set the MAR according to the PC
		MAR.setMemaddress(PC.getPCaddress());
		// PC += 1
		PC.PCPlus();
		// get the instruction from the Memory to MBR
		mfindex = Mem.readMem(MAR.getMemaddress()+7);
		checkaddress();
		MBR.setData(Mem.readMem(MAR.getMemaddress()+7));
		// set the instruction to IR from MBR
		IR.setinstruction(MBR.getData());
		// decode and get the opcode and then execute instruction accordingly
		switch(IR.getopcode()){
			case 1:
				Load();
				break;
			case 2:
				Store();
				break;
			case 0:
				Halt();
				break;
			case 3:
				LDA();
				break;
			case 33:
				LDX();
				break;
			case 34:
				STX();
				break;
			default:
				MFR.setFault(2);
				halt = 1;
		}
	}
	
	public void runinstruction() {
		// if the machine halts, the CPU will not work any more
		if (halt == 1) return;
		switch(IR.getopcode()){
		case 1:
			Load();
			break;
		case 2:
			Store();
			break;
		case 0:
			Halt();
			break;
		case 3:
			LDA();
			break;
		case 33:
			LDX();
			break;
		case 34:
			STX();
			break;
		default:
			MFR.setFault(2);
			halt = 1;
	}
	}
	
// This acts as the load instruction
	public void Load(){
		int EA = 0;
		// checks for an IR indirect in each register and computing the correct EA
		if (IR.getindirect() == 0) {
			if (IR.getindexregister() == 0) {
				EA = IR.getaddress();
			}
			else if (IR.getindexregister() > 0 && IR.getindexregister() < 4) {
				EA = IXR.getregister(IR.getindexregister()) + IR.getaddress();
			}
		}
		else if (IR.getindirect() == 1) {
			if (IR.getindexregister() == 0) {
				MAR.setMemaddress(IR.getaddress());
				mfindex = Mem.readMem(MAR.getMemaddress()+7);
				checkaddress();
				MBR.setData(Mem.readMem(MAR.getMemaddress()+7));
				EA = MBR.getData();
			}
			else if (IR.getindexregister() > 0 && IR.getindexregister() < 4) {
				MAR.setMemaddress(IXR.getregister(IR.getindexregister()) + IR.getaddress());
				mfindex = Mem.readMem(MAR.getMemaddress()+7);
				checkaddress();
				MBR.setData(Mem.readMem(MAR.getMemaddress()+7));
				EA = MBR.getData();
			}
		}
		// set the correct EA to the MAR
		MAR.setMemaddress(EA);
		// read the Memory and fetch the data to the MBR
		mfindex = Mem.readMem(MAR.getMemaddress()+7);
		checkaddress();
		MBR.setData(Mem.readMem(MAR.getMemaddress()+7));
		// load the data in MBR to the target register
		GPRs.setregister(IR.getregister(), MBR.getData());
	}
	
// Store instruction
	public void Store(){
		int EA = 0;
		// checks for an IR indirect in each register and computing the correct EA
		if (IR.getindirect() == 0) {
			if (IR.getindexregister() == 0) {
				EA = IR.getaddress();
			}
			else if (IR.getindexregister() > 0 && IR.getindexregister() < 4) {
				EA = IXR.getregister(IR.getindexregister()) + IR.getaddress();
			}
		}
		else if (IR.getindirect() == 1) {
			if (IR.getindexregister() == 0) {
				MAR.setMemaddress(IR.getaddress());
				mfindex = Mem.readMem(MAR.getMemaddress()+7);
				checkaddress();
				MBR.setData(Mem.readMem(MAR.getMemaddress()+7));
				EA = MBR.getData();
			}
			else if (IR.getindexregister() > 0 && IR.getindexregister() < 4) {
				MAR.setMemaddress(IXR.getregister(IR.getindexregister()) + IR.getaddress());
				mfindex = Mem.readMem(MAR.getMemaddress()+7);
				checkaddress();
				MBR.setData(Mem.readMem(MAR.getMemaddress()+7));
				EA = MBR.getData();
			}
		}
		// set the correct EA to the MAR
		MAR.setMemaddress(EA);
		// get the data from GPRs to MBR
		MBR.setData(GPRs.getregister(IR.getregister()));
		// write the data in MBR to the Memory with the address of MAR
		mfindex = Mem.writeMem(MAR.getMemaddress()+7, MBR.getData());
		checkaddress();
	}
	
	// This acts as the LDA instruction
	public void LDA(){
		int EA = 0;
		// checks for an IR indirect in each register and computing the correct EA
		if (IR.getindirect() == 0) {
			if (IR.getindexregister() == 0) {
				EA = IR.getaddress();
			}
			else if (IR.getindexregister() > 0 && IR.getindexregister() < 4) {
				EA = IXR.getregister(IR.getindexregister()) + IR.getaddress();
			}
		}
		else if (IR.getindirect() == 1) {
			if (IR.getindexregister() == 0) {
				MAR.setMemaddress(IR.getaddress());
				mfindex = Mem.readMem(MAR.getMemaddress()+7);
				checkaddress();
				MBR.setData(Mem.readMem(MAR.getMemaddress()+7));
				EA = MBR.getData();
			}
			else if (IR.getindexregister() > 0 && IR.getindexregister() < 4) {
				MAR.setMemaddress(IXR.getregister(IR.getindexregister()) + IR.getaddress());
				mfindex = Mem.readMem(MAR.getMemaddress()+7);
				checkaddress();
				MBR.setData(Mem.readMem(MAR.getMemaddress()+7));
				EA = MBR.getData();
			}
		}
		
		// load the EA to the target register
		GPRs.setregister(IR.getregister(), EA);
	}
	

	// This acts as the LDX instruction
	public void LDX(){
		int EA = 0;
		// checks for an IR indirect in each register and computing the correct EA
		if (IR.getindirect() == 0) {
			if (IR.getindexregister() == 0) {
				EA = IR.getaddress();
			}
			else if (IR.getindexregister() > 0 && IR.getindexregister() < 4) {
				EA = IXR.getregister(IR.getindexregister()) + IR.getaddress();
			}
		}
		else if (IR.getindirect() == 1) {
			if (IR.getindexregister() == 0) {
				MAR.setMemaddress(IR.getaddress());
				mfindex = Mem.readMem(MAR.getMemaddress()+7);
				checkaddress();
				MBR.setData(Mem.readMem(MAR.getMemaddress()+7));
				EA = MBR.getData();
			}
			else if (IR.getindexregister() > 0 && IR.getindexregister() < 4) {
				MAR.setMemaddress(IXR.getregister(IR.getindexregister()) + IR.getaddress());
				mfindex = Mem.readMem(MAR.getMemaddress()+7);
				checkaddress();
				MBR.setData(Mem.readMem(MAR.getMemaddress()+7));
				EA = MBR.getData();
			}
		}
		// set the correct EA to the MAR
		MAR.setMemaddress(EA);
		// read the Memory and fetch the data to the MBR
		mfindex = Mem.readMem(MAR.getMemaddress()+7);
		checkaddress();
		MBR.setData(Mem.readMem(MAR.getMemaddress()+7));
		// load the data in MBR to the target register
		IXR.setregister(IR.getindexregister(), MBR.getData());
	}
	
	// STX instruction
	public void STX(){
		int EA = 0;
		// checks for an IR indirect in each register and computing the correct EA
		if (IR.getindirect() == 0) {
			if (IR.getindexregister() == 0) {
				EA = IR.getaddress();
			}
			else if (IR.getindexregister() > 0 && IR.getindexregister() < 4) {
				EA = IXR.getregister(IR.getindexregister()) + IR.getaddress();
			}
		}
		else if (IR.getindirect() == 1) {
			if (IR.getindexregister() == 0) {
				MAR.setMemaddress(IR.getaddress());
				mfindex = Mem.readMem(MAR.getMemaddress()+7);
				checkaddress();
				MBR.setData(Mem.readMem(MAR.getMemaddress()+7));
				EA = MBR.getData();
			}
			else if (IR.getindexregister() > 0 && IR.getindexregister() < 4) {
				MAR.setMemaddress(IXR.getregister(IR.getindexregister()) + IR.getaddress());
				mfindex = Mem.readMem(MAR.getMemaddress()+7);
				checkaddress();
				MBR.setData(Mem.readMem(MAR.getMemaddress()+7));
				EA = MBR.getData();
			}
		}
		// set the correct EA to the MAR
		MAR.setMemaddress(EA);
		// get the data from GPRs to MBR
		MBR.setData(IXR.getregister(IR.getindexregister()));
		// write the data in MBR to the Memory with the address of MAR
		mfindex = Mem.writeMem(MAR.getMemaddress()+7, MBR.getData());
		checkaddress();
	}
	
// check the access of memory is write or not. If not, we go to set the MFR and get solution which is halt right now
	public void checkaddress() {
		if (mfindex == -1) {
			MFR.setFault(0);
			machinefault();
		}
		else if (mfindex == -2) {
			MFR.setFault(3);
			machinefault();
		}
	}

// deal with the machine fault
	public void machinefault() {
		if (MFR.getFault() >= 0 && MFR.getFault() < 4) {
			// find the solution's address which is 6
			MAR.setMemaddress(1);
			MBR.setData(Mem.CPUaccess(MAR.getMemaddress()));
			//get the solution instruction which is halt right now
			MAR.setMemaddress(MBR.getData());
			MBR.setData(Mem.CPUaccess(MAR.getMemaddress()));
			IR.setinstruction(MBR.getData());
			runinstruction();
		}
	}
	
	public void Halt() {
		halt = 1;
	}
}