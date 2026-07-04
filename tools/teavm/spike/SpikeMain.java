package nullpomino.teavmspike;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.randomizer.BagRandomizer;
import nullpomino.game.randomizer.MemorylessRandomizer;
import nullpomino.game.randomizer.NintendoRandomizer;
import nullpomino.game.randomizer.Randomizer;
import nullpomino.game.wallkick.ClassicWallkick;
import nullpomino.game.wallkick.StandardWallkick;
import nullpomino.game.wallkick.Wallkick;
import nullpomino.util.GeneralUtil;
import nullpomino.util.JdkRandom;

/**
 * TeaVM feasibility spike: exercises a representative slice of the game
 * logic (Field/Piece, wallkick, randomizer, GameEngine/GameManager, and the
 * Class.forName-based factory in GeneralUtil) with no logback on the
 * classpath, so a TeaVM compile/run failure here can't be confused with a
 * logback/Joran failure (see SpikeMainWithLogback).
 */
public final class SpikeMain {
	private SpikeMain() {}

	public static void main(String[] args) {
		exerciseGameLogic();
	}

	static void exerciseGameLogic() {
		System.out.println("=== SpikeMain start ===");
		fieldLineClear();
		randomDeterminism();
		randomizers();
		wallkicks();
		gameEngine();
		reflectiveFactory();
		System.out.println("=== SpikeMain done ===");
	}

	private static void randomDeterminism() {
		JdkRandom nonPowerOfTwo = new JdkRandom(12345L);
		StringBuilder seq7 = new StringBuilder();
		for(int i = 0; i < 10; i++) seq7.append(nonPowerOfTwo.nextInt(7)).append(' ');
		System.out.println("JdkRandom.nextInt(7): " + seq7);

		JdkRandom powerOfTwo = new JdkRandom(12345L);
		StringBuilder seq8 = new StringBuilder();
		for(int i = 0; i < 10; i++) seq8.append(powerOfTwo.nextInt(8)).append(' ');
		System.out.println("JdkRandom.nextInt(8): " + seq8);
	}

	private static void fieldLineClear() {
		Field field = new Field();
		int y = field.getHeight() - 1;
		for(int x = 0; x < field.getWidth(); x++) {
			field.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
		}
		int flagged = field.checkLine();
		int cleared = field.clearLine();
		System.out.println("Field: width=" + field.getWidth() + " height=" + field.getHeight()
				+ " flaggedLines=" + flagged + " clearedLines=" + cleared);
	}

	private static void randomizers() {
		boolean[] allEnabled = new boolean[Piece.PIECE_COUNT];
		for(int i = 0; i < allEnabled.length; i++) allEnabled[i] = true;

		Randomizer[] randomizers = {
				new BagRandomizer(),
				new NintendoRandomizer(),
				new MemorylessRandomizer(),
		};

		for(Randomizer r : randomizers) {
			r.setState(allEnabled, 12345L);
			StringBuilder seq = new StringBuilder();
			for(int i = 0; i < 10; i++) seq.append(r.next()).append(' ');
			System.out.println(r.getClass().getSimpleName() + ": " + seq);
		}
	}

	private static void wallkicks() {
		Field field = new Field();
		Piece piece = new Piece(Piece.PIECE_T);
		Wallkick[] wallkicks = {new StandardWallkick(), new ClassicWallkick()};

		for(Wallkick w : wallkicks) {
			WallkickResult result = w.executeWallkick(4, 4, 1, 0, 1, true, piece, field, null);
			System.out.println(w.getClass().getSimpleName() + ": result=" + (result == null ? "null (no kick)" : "kicked"));
		}
	}

	private static void gameEngine() {
		GameManager manager = new GameManager(new EventReceiver());
		GameEngine engine = new GameEngine(manager, 0);
		engine.init();
		engine.field = new Field();
		System.out.println("GameEngine: playerID=" + engine.playerID
				+ " versionMajor=" + engine.versionMajor
				+ " randSeed=" + Long.toHexString(engine.randSeed));
	}

	private static void reflectiveFactory() {
		Wallkick w = GeneralUtil.loadWallkick("nullpomino.game.wallkick.StandardWallkick");
		System.out.println("GeneralUtil.loadWallkick (Class.forName-based): "
				+ (w == null ? "FAILED (null)" : "OK -> " + w.getClass().getName()));
	}
}
