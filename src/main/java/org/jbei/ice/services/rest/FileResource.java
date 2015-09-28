package org.jbei.ice.services.rest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jbei.ice.lib.account.SessionHandler;
import org.jbei.ice.lib.bulkupload.FileBulkUpload;
import org.jbei.ice.lib.common.logging.Logger;
import org.jbei.ice.lib.dao.DAOFactory;
import org.jbei.ice.lib.dto.ConfigurationKey;
import org.jbei.ice.lib.dto.Setting;
import org.jbei.ice.lib.dto.entry.AttachmentInfo;
import org.jbei.ice.lib.dto.entry.EntryType;
import org.jbei.ice.lib.dto.entry.SequenceInfo;
import org.jbei.ice.lib.entry.EntrySelection;
import org.jbei.ice.lib.entry.attachment.AttachmentController;
import org.jbei.ice.lib.entry.model.Entry;
import org.jbei.ice.lib.entry.sequence.ByteArrayWrapper;
import org.jbei.ice.lib.entry.sequence.SequenceAnalysisController;
import org.jbei.ice.lib.entry.sequence.SequenceController;
import org.jbei.ice.lib.entry.sequence.composers.pigeon.PigeonSBOLv;
import org.jbei.ice.lib.models.Sequence;
import org.jbei.ice.lib.models.TraceSequence;
import org.jbei.ice.lib.net.RemoteEntries;
import org.jbei.ice.lib.utils.EntriesAsCSV;
import org.jbei.ice.lib.utils.Utils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.net.URI;
import java.nio.file.Paths;

/**
 * @author Hector Plahar
 */
@Path("/file")
public class FileResource extends RestResource {

    private SequenceController sequenceController = new SequenceController();
    private AttachmentController attachmentController = new AttachmentController();

    /**
     * @return Response with attachment info on uploaded file
     */
    @POST
    @Path("attachment")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(@FormDataParam("file") InputStream fileInputStream,
                         @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
                         @HeaderParam("X-ICE-Authentication-SessionId") String sessionId) {
        try {
            final String fileName = contentDispositionHeader.getFileName();
            final String fileId = Utils.generateUUID();
            final File attachmentFile = Paths.get(
                    Utils.getConfigValue(ConfigurationKey.DATA_DIRECTORY),
                    AttachmentController.attachmentDirName, fileId).toFile();
            FileUtils.copyInputStreamToFile(fileInputStream, attachmentFile);
            final AttachmentInfo info = new AttachmentInfo();
            info.setFileId(fileId);
            info.setFilename(fileName);
            return Response.status(Response.Status.OK).entity(info).build();
        } catch (final IOException e) {
            Logger.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    protected Response addHeaders(Response.ResponseBuilder response, String fileName) {
        response.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        int dotIndex = fileName.lastIndexOf('.') + 1;
        if (dotIndex == 0)
            return response.build();

        String mimeType = ExtensionToMimeType.getMimeType(fileName.substring(dotIndex));
        response.header("Content-Type", mimeType + "; name=\"" + fileName + "\"");
        return response.build();
    }

    /**
     * Retrieves a temp file by fileId
     */
    @GET
    @Path("tmp/{fileId}")
    public Response getTmpFile(@PathParam("fileId") final String fileId) {
        final File tmpFile = Paths.get(Utils.getConfigValue(ConfigurationKey.TEMPORARY_DIRECTORY),
                fileId).toFile();
        if (tmpFile == null || !tmpFile.exists()) {
            return super.respond(Response.Status.NOT_FOUND);
        }
        return addHeaders(Response.ok(tmpFile), tmpFile.getName());
    }

    @GET
    @Path("attachment/{fileId}")
    public Response getAttachment(@PathParam("fileId") String fileId,
                                  @QueryParam("sid") String sid,
                                  @HeaderParam("X-ICE-Authentication-SessionId") String sessionId) {
        if (StringUtils.isEmpty(sessionId))
            sessionId = sid;

        String userId = getUserId(sessionId);
        File file = attachmentController.getAttachmentByFileId(userId, fileId);
        if (file == null) {
            return respond(Response.Status.NOT_FOUND);
        }

        String name = attachmentController.getFileName(userId, fileId);
        return addHeaders(Response.ok(file), name);
    }

    @GET
    @Path("remote/{id}/attachment/{fileId}")
    public Response getRemoteAttachment(@PathParam("id") long partnerId,
                                        @PathParam("fileId") String fileId,
                                        @QueryParam("sid") String sid,
                                        @HeaderParam("X-ICE-Authentication-SessionId") String sessionId) {
        String userId = getUserId(sessionId);
        RemoteEntries entries = new RemoteEntries();
        File file = entries.getPublicAttachment(userId, partnerId, fileId);
        if (file == null)
            return respond(Response.Status.NOT_FOUND);

        return addHeaders(Response.ok(file), "remoteAttachment");
    }

    @GET
    @Path("upload/{type}")
    public Response getUploadCSV(@PathParam("type") final String type,
                                 @QueryParam("link") final String linkedType) {
        EntryType entryAddType = EntryType.nameToType(type);
        EntryType linked;
        if (linkedType != null) {
            linked = EntryType.nameToType(linkedType);
        } else {
            linked = null;
        }

        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream output) throws IOException, WebApplicationException {
                byte[] template = FileBulkUpload.getCSVTemplateBytes(entryAddType, linked,
                        "existing".equalsIgnoreCase(linkedType));
                ByteArrayInputStream stream = new ByteArrayInputStream(template);
                IOUtils.copy(stream, output);
            }
        };

        String filename = type.toLowerCase();
        if (linkedType != null) {
            filename += ("_" + linkedType.toLowerCase());
        }

        return addHeaders(Response.ok(stream), filename + "_csv_upload.csv");
    }

    @GET
    @Path("{partId}/sequence/{type}")
    public Response downloadSequence(
            @PathParam("partId") final long partId,
            @PathParam("type") final String downloadType,
            @QueryParam("sid") String sid,
            @HeaderParam("X-ICE-Authentication-SessionId") String sessionId) {
        if (StringUtils.isEmpty(sessionId))
            sessionId = sid;

        final String userId = getUserId(sessionId);
        final ByteArrayWrapper wrapper = sequenceController.getSequenceFile(userId, partId, downloadType);

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(final OutputStream output) throws IOException,
                    WebApplicationException {
                final ByteArrayInputStream stream = new ByteArrayInputStream(wrapper.getBytes());
                IOUtils.copy(stream, output);
            }
        };

        return addHeaders(Response.ok(stream), wrapper.getName());
    }

    @GET
    @Path("trace/{fileId}")
    public Response getTraceSequenceFile(@PathParam("fileId") String fileId,
                                         @QueryParam("sid") String sid,
                                         @HeaderParam("X-ICE-Authentication-SessionId") String sessionId) {
        final SequenceAnalysisController sequenceAnalysisController = new SequenceAnalysisController();
        final TraceSequence traceSequence = sequenceAnalysisController.getTraceSequenceByFileId(fileId);
        if (traceSequence != null) {
            final File file = sequenceAnalysisController.getFile(traceSequence);
            return addHeaders(Response.ok(file), traceSequence.getFilename());
        }
        return Response.serverError().build();
    }

    @GET
    @Produces("image/png")
    @Path("sbolVisual/{rid}")
    public Response getSBOLVisual(@PathParam("rid") String recordId,
                                  @HeaderParam("X-ICE-Authentication-SessionId") String sessionId) {
        final String tmpDir = Utils.getConfigValue(ConfigurationKey.TEMPORARY_DIRECTORY);
        final Entry entry = DAOFactory.getEntryDAO().getByRecordId(recordId);
        final Sequence sequence = entry.getSequence();
        final String hash = sequence.getFwdHash();
        final File png = Paths.get(tmpDir, hash + ".png").toFile();

        if (png.exists()) {
            return addHeaders(Response.ok(png), entry.getPartNumber() + ".png");
        }

        final URI uri = PigeonSBOLv.generatePigeonVisual(sequence);
        if (uri != null) {
            try (final InputStream in = uri.toURL().openStream();
                 final OutputStream out = new FileOutputStream(png);) {
                IOUtils.copy(in, out);
            } catch (IOException e) {
                Logger.error(e);
                return respond(false);
            }

            return addHeaders(Response.ok(png), entry.getPartNumber() + ".png");
        }
        return respond(false);
    }

    /**
     * this creates an entry if an id is not specified in the form data
     */
    @POST
    @Path("sequence")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadSequence(@FormDataParam("file") InputStream fileInputStream,
                                   @FormDataParam("entryRecordId") String recordId,
                                   @FormDataParam("entryType") String entryType,
                                   @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
                                   @HeaderParam("X-ICE-Authentication-SessionId") String sessionId) {
        try {
            if (entryType == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }

            final String fileName = contentDispositionHeader.getFileName();
            final String userId = SessionHandler.getUserIdBySession(sessionId);
            final String sequence = IOUtils.toString(fileInputStream);
            final SequenceInfo sequenceInfo = sequenceController.parseSequence(userId, recordId,
                    entryType, sequence, fileName);
            if (sequenceInfo == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.status(Response.Status.OK).entity(sequenceInfo).build();
        } catch (final Exception e) {
            Logger.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Extracts the csv information and writes it to the temp dir and returns the file uuid. Then
     * the client is expected to make another rest call with the uuid in a separate window. This
     * workaround is due to not being able to download files using XHR or sumsuch
     */
    @POST
    @Path("csv")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response downloadCSV(
            @HeaderParam("X-ICE-Authentication-SessionId") String sessionId,
            EntrySelection selection) {
        String userId = super.getUserId(sessionId);
        EntriesAsCSV entriesAsCSV = new EntriesAsCSV();
        boolean success = entriesAsCSV.setSelectedEntries(userId, selection);
        if (!success)
            return super.respond(false);

        final File file = entriesAsCSV.getFilePath().toFile();
        if (file.exists()) {
            return Response.ok(new Setting("key", file.getName())).build();
        }

        return respond(false);
    }
}
