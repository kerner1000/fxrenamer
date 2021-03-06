package com.github.drrename;

import java.io.File;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import com.github.drrename.strategy.RenamingStrategy;
import com.github.ktools1000.io.BackupCreator;

public abstract class AbstractRenamingStrategyTest {

	@Before
	public void setUp() throws Exception {

		if((renamedFile != null) && renamedFile.toFile().exists())
			FileUtils.deleteQuietly(renamedFile.toFile());
		backupFile = new BackupCreator().makeBackup(getTestFile());
		s = getStrategy();
	}

	@After
	public void tearDown() throws Exception {

		new BackupCreator().restoreBackup(backupFile);
		FileUtils.deleteQuietly(backupFile);
		backupFile = null;
		s = null;
		if((renamedFile != null) && renamedFile.toFile().exists())
			FileUtils.deleteQuietly(renamedFile.toFile());
	}

	protected File backupFile;
	protected Path renamedFile;
	protected RenamingStrategy s;

	protected abstract RenamingStrategy getStrategy();

	protected abstract File getTestFile();
}
