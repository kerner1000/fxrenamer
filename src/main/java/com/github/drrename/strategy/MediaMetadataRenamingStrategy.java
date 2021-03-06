package com.github.drrename.strategy;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

/**
 * A {@link RenamingStrategy} that renames files based on the media-meta
 * information, if available.
 *
 * @author Alexander Kerner
 *
 */
public class MediaMetadataRenamingStrategy extends RenamingStrategyProto {

    private static final Logger logger = LoggerFactory.getLogger(MediaMetadataRenamingStrategy.class);
    /**
     * The default date format for the new file name.
     */
    public static final DateTimeFormatter DEFAULT_DATE_FORMATTER_WRITE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    /**
     * Date formats for reading the media-tag date string.
     */
    public static final List<DateTimeFormatter> DEFAULT_DATE_FORMATTERS_READ = Arrays.asList(
	    DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"),
	    DateTimeFormatter.ofPattern("EE MMM dd HH:mm:ss XXX yyyy"),
	    DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss XXX yyyy"),
	    DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy"),
	    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"));

    private DateTimeFormatter dateFormatterWrite;

    private List<DateTimeFormatter> dateFormattersRead;

    public MediaMetadataRenamingStrategy() {

	dateFormatterWrite = DEFAULT_DATE_FORMATTER_WRITE;
	dateFormattersRead = DEFAULT_DATE_FORMATTERS_READ;
	setAdditionalParam(buildAdditionalParam());
    }

    @Override
    public String getIdentifier() {

	return "Date from Metadata";
    }

    private AdditionalParam buildAdditionalParam() {
	final AdditionalParam result = new AdditionalParam("Fallback to\nfile mod. date",
		"When proper date could not be parsed from metadata,\nrename by last modification date.\n Will be applied, if value is 'true'.");
	result.setValue("false");
	return result;
    }

    @Override
    public String getNameNew(final Path file) throws IOException, InterruptedException {

	if (Thread.currentThread().isInterrupted())
	    throw new InterruptedException("Interrupted");
	try {
	    final Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());
	    for (final Directory directory : metadata.getDirectories()) {
		// fallback tag that is used to parse the date from, if 'primary' tag cannot be
		// found
		Tag fallbackTag = null;

		for (final Tag tag : directory.getTags()) {

		    // look for a creation date
		    if (tag.toString().toLowerCase().contains("creation"))
			return getNewFileName(tag.getDescription(), file);

		    // if applicable, check for the fallback tag
		    else if (getAdditionalParam().isPresent()
			    && Boolean.parseBoolean(getAdditionalParam().get().getValue())
			    && tag.toString().toLowerCase().contains("date")) {
			fallbackTag = tag;
		    }
		}
		if (fallbackTag != null) {
		    if (logger.isDebugEnabled()) {
			logger.debug("No suitable tag found, using fallback tag " + fallbackTag);
		    }
		    return getNewFileName(fallbackTag.getDescription(), file);
		}
	    }
	} catch (final ImageProcessingException e) {
	    if (logger.isDebugEnabled()) {
		logger.debug(file.getFileName() + ": " + e.getLocalizedMessage());
	    }
	} catch (final Exception e) {
	    throw new IOException(e);
	}
	return file.getFileName().toString();
    }

    private String getNewFileName(final String tagDescription, final Path file) {

	final String fileName = file.getFileName().toString();
	final String newFileName = processTag(tagDescription, fileName);
	if (fileName.equals(newFileName))
	    return fileName;
	return newFileName;
    }

    /**
     *
     *
     * @param tagDescription
     *            the input string from which a date is parsed
     * @param fileName
     *            the file name that is returned if parsing a date fails
     * @return the extracted, date-based file name or {@code fileName} if parsing
     *         fails
     */
    String processTag(final String tagDescription, final String fileName) {

	final String fileExtension = FilenameUtils.getExtension(fileName);

	final String newDateString = processDateString(getDateFormattersRead(), getDateFormatterWrite(),
		tagDescription);
	if (newDateString != null)
	    // the file name has changed, return the new file name and re-append the file
	    // extension
	    return (fileExtension != null) && (fileExtension.length() > 0) ? newDateString + "." + fileExtension
		    : newDateString;
	if (logger.isDebugEnabled()) {
	    logger.debug(fileName + ": Failed to parse date from " + tagDescription);
	}
	return fileName;
    }

    static String processDateString(final Collection<DateTimeFormatter> dateFormattersRead,
	    final DateTimeFormatter dateFormatWrite, final String dateString) {

	if ((dateFormattersRead == null) || dateFormattersRead.isEmpty())
	    throw new IllegalStateException("No read-parser available");

	for (final DateTimeFormatter dateFormatterRead : dateFormattersRead) {
	    try {
		return processDateString(dateFormatterRead, dateFormatWrite, dateString);
	    } catch (final DateTimeParseException e) {
		// ignore this tag
	    }
	}
	return null;
    }

    static String processDateString(final DateTimeFormatter dateFormatRead, final DateTimeFormatter dateFormatWrite,
	    final String dateString) {

	if (dateFormatRead == null)
	    throw new IllegalStateException("No read-parser available");
	if (dateFormatWrite == null)
	    throw new IllegalStateException("No write-parser available");
	final TemporalAccessor dt = dateFormatRead.parse(dateString);
	final String result = dateFormatWrite.format(dt);
	return result;
    }

    @Override
    public boolean isReplacing() {

	return false;
    }

    // Getter / Setter //

    public List<DateTimeFormatter> getDateFormattersRead() {
	return dateFormattersRead;
    }

    public void setDateFormattersRead(final List<DateTimeFormatter> dateFormattersRead) {
	this.dateFormattersRead = dateFormattersRead;
    }

    public DateTimeFormatter getDateFormatterWrite() {

	return dateFormatterWrite;
    }

    public void setDateFormatterWrite(final DateTimeFormatter dateFormatterWrite) {

	this.dateFormatterWrite = dateFormatterWrite;
    }

}
