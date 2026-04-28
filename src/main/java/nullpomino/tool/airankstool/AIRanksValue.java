package nullpomino.tool.airankstool;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

public class AIRanksValue {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Ranks ranks;
		String inputFile=AIRanksConstants.RANKSAI_DIR+"ranks20";

		if (inputFile.trim().length() == 0)
			ranks=new Ranks(4,9);
		else {
			try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile))) {
				ranks = (Ranks)in.readObject();
				int [] surface1={	0, 1, 1, -1, -1, 1, -3, -2};
				int [] surface2={ 	0, 1, 1, -1, -1, 4, -4, 2};
				
				
				int rank1=ranks.getRankValue(ranks.encode(surface1));
				int rank2=ranks.getRankValue(ranks.encode(surface2));
				System.out.println(rank1);
				System.out.println(rank2);
			} catch (FileNotFoundException e) {
				ranks=new Ranks(4,9);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}


	

}
