package com.github.drrename.strategy;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RenamingStrategyProto implements RenamingStrategy {

	private static final Logger logger = LoggerFactory.getLogger(RenamingStrategyProto.class);
	private final static Pattern pattern = Pattern.compile(".*_(\\d*)$");
	private String replacementStringFrom = "";
	private String replacementStringTo = "";
	private AdditionalParam additionalParam;

	/**
	 * Performs the rename. Does not override existing files, but creates
	 * numbered suffixes for file names that exist already.
	 *
	 * @param file
	 *            the file to rename
	 * @param nameNew
	 *            the new file name
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected Path doRename(final Path file, String nameNew) throws IOException, InterruptedException {

		if(Thread.currentThread().isInterrupted())
			throw new InterruptedException("Cancelled");
		int fileNameCounter = 1;
		try {
			final String nameOld = getNameOld(file);
			if(nameOld.equals(nameNew)) {
				if(logger.isDebugEnabled()) {
					logger.debug("Skipping '" + nameOld + "'");
				}
				return file;
			}
			if(logger.isDebugEnabled()) {
				logger.debug("Renaming" + IOUtils.LINE_SEPARATOR + "old:\t" + nameOld + IOUtils.LINE_SEPARATOR + "new:\t" + nameNew);
			}
			return Files.move(file, file.resolveSibling(nameNew));
		} catch(final FileAlreadyExistsException e) {
			final String extension = FilenameUtils.getExtension(nameNew);
			String baseName = FilenameUtils.getBaseName(nameNew);
			final Matcher matcher = pattern.matcher(baseName);
			if(matcher.matches()) {
				final String group = matcher.group(1);
				final int index = matcher.start(1);
				baseName = baseName.substring(0, index - 1); // also omit '_'
				// string
				fileNameCounter = Integer.parseInt(group);
			}
			fileNameCounter++;
			nameNew = baseName + "_" + fileNameCounter + "." + extension;
			return doRename(file, nameNew);
		}
	}

	@Override
	public String getHelpText() {

		return getIdentifier();
	}

	String getNameOld(final Path file) {

		return file.getFileName().toString();
	}

	public String getReplacementStringFrom() {

		return replacementStringFrom;
	}

	public String getReplacementStringTo() {

		return replacementStringTo;
	}

	@Override
	public Path rename(final Path file, final BasicFileAttributes attrs) throws IOException, InterruptedException {

		return doRename(file, getNameNew(file));
	}

	@Override
	public void setReplacementStringFrom(final String replacementStringFrom) {

		this.replacementStringFrom = replacementStringFrom;
	}

	@Override
	public void setReplacementStringTo(final String replacementStringTo) {

		this.replacementStringTo = replacementStringTo;
	}

	@Override
	public String toString() {

		return getIdentifier();
	}

	@Override
	public Optional<AdditionalParam> getAdditionalParam() {

		return Optional.ofNullable(additionalParam);
	}

	public void setAdditionalParam(final AdditionalParam additionalParam) {

		this.additionalParam = additionalParam;
	}

	@Override
	public boolean hasAdditionalParam() {

		return getAdditionalParam().isPresent();
	}
}
