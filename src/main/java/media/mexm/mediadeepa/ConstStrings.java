/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa;

public interface ConstStrings {// NOSONAR

	String TEMPORAL_INFO = "Temporal Info";
	String SPATIAL_INFO = "Spatial Info";
	String SPATIAL_TEMPORAL_INFORMATION = "Spatial/Temporal Information";
	String TEMPORAL_INFORMATION = "Temporal Information";
	String SPATIAL_INFORMATION = "Spatial Information";

	String LOUDNESS_RANGE = "Loudness Range";
	String INTEGRATED = "Integrated";
	String TRUE_PEAK = "True Peak";
	String SAMPLE_PEAK = "Sample Peak";
	String LOUDNESS_RANGE_HIGH = "Loudness Range High";
	String LOUDNESS_RANGE_LOW = "Loudness Range Low";
	String LOUDNESS_RANGE_THRESHOLD = "Loudness Range Threshold";
	String INTEGRATED_THRESHOLD = "Integrated Threshold";

	String STREAM_INDEX = "Stream index";
	String VALUE = "Value";
	String FRAME = "Frame";
	String FORMAT = "Format";
	String STREAM = "Stream";

	String PTS = "Pts";
	String PTS_TIME = "Pts time";
	String PKT_SIZE = "Pkt size";
	String PKT_POS = "Pkt pos";
	String PKT_DURATION_TIME = "Pkt duration time";
	String PKT_DURATION = "Pkt duration";
	String PKT_DTS_TIME = "Pkt dts time";
	String PKT_DTS = "Pkt dts";
	String BEST_EFFORT_TIMESTAMP_TIME = "Best effort timestamp time";
	String BEST_EFFORT_TIMESTAMP = "Best effort timestamp";
	String NB_SAMPLES = "Nb samples";
	String MEDIA_TYPE = "Media type";

	String JAVA_CLASS = "Java class";
	String SETUP = "Setup";
	String NAME = "Name";
	String TYPE = "Type";

	String OTHER = "Other";
	String PEAK_COUNT = "Peak count";
	String PEAK_LEVEL = "Peak level";
	String NOISE_FLOOR_COUNT = "Noise floor count";
	String NOISE_FLOOR = "Noise floor";
	String FLATNESS = "Flatness";
	String ENTROPY = "Entropy";
	String DC_OFFSET = "DC Offset";
	String CHANNEL = "Channel";

	String CHANNEL_LAYOUT = "Channel layout";
	String CAN_CONTAIN = "Can contain";
	String BYTE_S_SEC = "byte(s)/sec";
	String MILLISECOND_S = "millisecond(s)";
	String EVENT_S = "event(s)";
	String PIXEL_S = "pixel(s)";
	String FRAMES = "frames";
	String COUNT = "Count";
	String BYTES_SECONDS = "bytes/seconds";
	String BYTES = "bytes";
	String FRAME_S = "frame(s)";
	String INTERLACING_FRAME_STATUS = "Interlacing frame status";
	String SAMPLE_S = "sample(s)";
	String DBFS = "dBFS";

	String REF_PKT_POS = "Ref pkt pos";
	String REF_DURATION_TIME = "Ref duration time";
	String REF_DURATION = "Ref duration";
	String REF_BEST_EFFORT_TIMESTAMP_TIME = "Ref best effort timestamp time";
	String REF_BEST_EFFORT_TIMESTAMP = "Ref best effort timestamp";
	String REF_PKT_DTS_TIME = "Ref pkt dts time";
	String REF_PKT_DTS = "Ref pkt dts";
	String REF_PTS_TIME = "Ref pts time";
	String REF_PTS = "Ref pts";
	String SAMPLE_FORMAT = "Sample format";
	String CHANNELS = "Channels";

	String B_FRAMES_DATA_SIZE = "B frames data size";
	String P_FRAMES_DATA_SIZE = "P frames data size";
	String I_FRAME_DATA_SIZE = "I frame data size";
	String GOP_DATA_SIZE = "GOP data size";
	String B_FRAMES_COUNT = "B frames count";
	String P_FRAMES_COUNT = "P frames count";
	String GOP_FRAME_COUNT = "GOP frame count";

	String I_FRAMES_SIZE_IN_GOP = "I frames size in GOP";
	String B_FRAMES_SIZE_IN_GOP = "B frames size in GOP";
	String P_FRAMES_SIZE_IN_GOP = "P frames size in GOP";
	String GOP_FRAME_SIZE_KBYTES = "GOP frame size (kbytes)";
	String VIDEO_FRAME_COUNT_BY_GOP = "Video frame count by GOP";
	String B_FRAME_COUNT_BY_GOP = "B frame count by GOP";
	String P_FRAME_COUNT_BY_GOP = "P frame count by GOP";
	String NUMBER_OF_FRAMES = "Number of frames";
	String ALL_I_SIZE = "All I size";
	String I_FRAMES_SIZE_IN_GOPS = "I frames size in GOPs";
	String I_FRAMES_COUNT = "I frames count";
	String GOPS_SIZE = "GOPs size";
	String GOPS_LENGTH = "GOPs length";

	String TRUE_PEAK_R = "True-peak R";
	String TRUE_PEAK_L = "True-peak L";
	String TRUE_PEAK_PER_FRAME_R = "True-peak per frame R";
	String TRUE_PEAK_PER_FRAME_L = "True-peak per frame L";
	String SAMPLE_PEAK_R = "Sample-peak R";
	String SAMPLE_PEAK_L = "Sample-peak L";
	String SHORT_TERM = "Short-term";
	String MOMENTARY = "Momentary";
	String POSITION = "Position";

	String DURATION = "Duration";
	String END = "End";
	String START = "Start";
	String SCOPE_CHANNEL = "Scope/Channel";

	String LINE = "Line";
	String CHAIN_POS = "Chain pos";
	String FILTER_NAME = "Filter name";

	String REPEATED_NEITHER = "Repeated neither";
	String REPEATED_BOTTOM = "Repeated bottom";
	String REPEATED_TOP = "Repeated top";
	String REPEATED_CURRENT_FRAME = "Repeated current frame";
	String MULTIPLE_UNDETERMINED = "Multiple undetermined";
	String MULTIPLE_PROGRESSIVE = "Multiple progressive";
	String MULTIPLE_CURRENT_FRAME = "Multiple current frame";
	String MULTIPLE_BOTTOM_FIELD_FIRST = "Multiple bottom field first";
	String MULTIPLE_TOP_FIELD_FIRST = "Multiple top field first";
	String SINGLE_UNDETERMINED = "Single undetermined";
	String SINGLE_PROGRESSIVE = "Single progressive";
	String SINGLE_CURRENT_FRAME = "Single current frame";
	String SINGLE_BOTTOM_FIELD_FIRST = "Single bottom field first";
	String SINGLE_TOP_FIELD_FIRST = "Single top field first";

	String FLAGS = "Flags";
	String POS = "Pos";
	String SIZE = "Size";
	String DURATION_TIME = "Duration time";
	String DTS_TIME = "Dts time";
	String DTS = "Dts";
	String CODEC_TYPE = "Codec type";

	String MIN = "Min";
	String MAX = "Max";
	String AVERAGE = "Average";

	String DISPLAY_PICTURE_NUMBER = "Display picture number";
	String CODED_PICTURE_NUMBER = "Coded picture number";
	String COLOR_SPACE = "Color space";
	String COLOR_TRANSFER = "Color transfer";
	String COLOR_PRIMARIES = "Color primaries";
	String COLOR_RANGE = "Color range";
	String PIX_FMT = "Pix fmt";
	String INTERLACED_FRAME = "Interlaced frame";
	String TOP_FIELD_FIRST = "Top field first";
	String HEIGHT = "Height";
	String WIDTH = "Width";

	String REPEAT_PICT = "Repeat pict";
	String PICT_TYPE = "Pict type";
	String KEY_FRAME = "Key frame";

	String SIDE_DATA = "Side data";
	String HZ_SEC = "Hz/sec";
	String CHANNEL_S = "channel(s)";
	String PACKET_S = "packet(s)";
	String NB_READ_PACKETS = "Nb read packets";
	String NB_READ_FRAMES = "Nb read frames";
	String CHANNEL_COUNT = "Channel count";
	String SAMPLE_RATE = "Sample rate";
	String REFS = "Refs";
	String FIELD_ORDER = "Field order";
	String CODEC_LEVEL = "Codec level";
	String CODEC_PROFILE = "Codec Profile";
	String COLOR_TRANSFERT = "Color transfert";
	String CHROMA_LOCATION = "Chroma location";
	String PIXEL_FORMAT = "Pixel format";
	String DISPLAY_ASPECT_RATIO = "Display aspect ratio";
	String SAMPLE_ASPECT_RATIO = "Sample aspect ratio";
	String HAS_B_FRAMES = "Has B frames";
	String CODED_HEIGHT = "Coded height";
	String CODED_WIDTH = "Coded width";
	String TIME_BASE = "Time base";
	String DURATION_TS = "Duration TS";
	String FRAME_COUNT = "Frame count";
	String INITIAL_PADDING = "Initial padding";
	String START_TIME = "Start time";
	String START_PTS = "Start pres. TS";
	String FILM_GRAIN = "Film grain";
	String CLOSED_CAPTIONS = "Closed captions";
	String EXTRADATA = "Extradata";
	String AVERAGE_FRAME_RATE = "Average frame rate";
	String REAL_FRAME_RATE = "Real frame rate";
	String BIT_S = "bit(s)";
	String BITS_PER_SAMPLE = "Bits per sample";
	String BITS_PER_RAW_SAMPLE = "Bits per raw sample";
	String MAX_BITRATE_INDICATED = "Max bitrate (indicated)";
	String BITRATE_INDICATED = "Bitrate (indicated)";
	String DISPOSITION = "Disposition";
	String CODEC_TAG = "Codec tag";
	String CODEC_INTERNAL_NAME = "Codec internal name";
	String CODEC_NAME = "Codec name";

	String SAMPLES = "samples";
	String MILLISECONDS = "milliseconds";
	String FRAME_DURATION = "Frame duration";
	String FRAME_LENGTH = "Frame length";
	String FRAME_SIZE = "Frame size";
	String REPEAT_COUNT = "Repeat count";
	String KEY_COUNT = "Key count";
	String FRAME_BEST_EFFORT_TIME = "Frame best effort time";
	String FRAME_DTS_TIME = "Frame DTS time";
	String FRAME_PTS_TIME = "Frame PTS time";

	String FREEZE_STATIC_FRAMES = "Freeze (static) frames";
	String BLACK_FRAMES = "Black frames";
	String AUDIO_MONO = "Audio mono";
	String FREEZE_FRAMES = "Freeze frames";
	String AUDIO_SILENCE = "Audio silence";
	String FULL_BLACK_FRAMES = "Full black frames";

	String LABEL_MAXIMUM = "Maximum";
	String LABEL_MEDIAN = "Median";
	String LABEL_AVERAGE = "Average";
	String LABEL_MINIMUM = "Minimum";

	String ABOUT_THIS_DOCUMENT = "About this document";
	String DOCUMENT_CREATION_DATE = "Document creation date";
	String TARGET_SOURCE = "Target source";
	String MEDIADEEPA_REPORT_DOCUMENT = "Mediadeepa report document";
	String TO_TOP_ICON = "⇑";

	String PHASE_CORRELATION = "Phase correlation";
	String VIDEO_FRAMES = "Video frames";
	String VIDEO_MEDIA_FILE_INFORMATION = "Video media file information";

	String IMAGE_AND_MOTION_COMPLEXITY = "Image and motion complexity";

	String ALL_STREAMS_BITRATES = "All streams bitrates";
	String OTHER_BITRATE = "Other bitrate";
	String DATA_BITRATE = "Data bitrate";
	String AUDIO_BITRATE = "Audio bitrate";
	String VIDEO_BITRATE = "Video bitrate";
	String STREAM_PACKETS = "Stream packets";

	String PROGRAM_S = "program(s)";
	String BYTE_S = "byte(s)";
	String CHAPTERS = "Chapters";
	String STREAM_S = "stream(s)";
	String CHAPTER_S = "chapter(s)";
	String ALL_STREAM_COUNT = "All stream count";
	String PROGRAM_COUNT = "Program count";
	String CONTAINER_START_TIME = "Container start time";
	String CONTAINER_DECLATED_BITRATE = "Container declated bitrate";
	String FILE_SIZE = "File size";
	String FILE_FORMAT = "File format";
	String MEDIA_CONTAINER = "Media container";

	String INTERLACING_DETECTION = "Interlacing detection";
	String VIDEO_COMPRESSION_GROUP_OF_PICTURES = "Video compression group-of-pictures";
	String LOUDNESS_EBU_R128 = "Loudness EBU-R128";
	String LOUDNESS_EBU_R128_SUMMARY = "Loudness EBU-R128 summary";
	String BLACK_BORDERS_CROP_DETECTION = "Black borders / crop detection";
	String IMAGE_BLUR_DETECTION = "Image blur detection";
	String IMAGE_COMPRESSION_ARTIFACT_DETECTION = "Image compression artifact detection";
	String SIGNAL_STATS = "Signal stats";
	String AUDIO_FRAMES = "Audio frames";
	String AUDIO_WAVEFORM = "Audio waveform";
	String AUDIO_MEDIA_FILE_INFORMATION = "Audio media file information";
	String FFMPEG_FILTERS_USED_IN_THIS_MEASURE = "FFmpeg filters used in this measure";

	String COMMAND_LINES_USED_TO_CREATE_THIS_REPORT = "Command lines used to create this report";
	String REPORT_CREATED_BY = "Report created by";
	String ANALYSIS_CREATED_BY = "Analysis created by";

	String FIRST_CROP_VALUES = "First crop values";
	String BACK_TO_FULL_FRAME_NO_CROP = "Back to full frame (no crop)";
	String BLACK_FRAME_FULL_CROP = "Black frame (full crop)";

	String UNKNOW = "(unknow)";
	String PROGRESSIVE = "progressive";
	String INTERLACED_BOTTOM_FIELD_FIRST = "Interlaced, bottom field first";
	String INTERLACED_TOP_FIELD_FIRST = "Interlaced, top field first";
	String PIXEL_ASPECT_RATIO = "Pixel aspect ratio";
	String STORAGE_ASPECT_RATIO = "Storage aspect ratio";
	String PIXEL_SURFACE = "Pixel surface";
	String IMAGE_RESOLUTION = "Image resolution";
	String FRAME_DURATION_DECLARED = "Frame duration (declared)";
	String FRAMES_ALL_ARE_KEY_FRAMES_NO_GOP = "frames (all are key frames, no GOP)";
	String REFS_MIN = "refs";

	String NO_REPEATED_FIELD = "No repeated field";
	String COULD_NOT_BE_CLASSIFIED_USING_MULTIPLE_FRAME_DETECTION = "Could not be classified using multiple-frame detection";
	String COULD_NOT_BE_CLASSIFIED_USING_SINGLE_FRAME_DETECTION = "Could not be classified using single-frame detection";
	String WITH_THE_BOTTOM_FIELD_REPEATED_FROM_THE_PREVIOUS_FRAME_S_BOTTOM_FIELD = "With the bottom field repeated from the previous frame’s bottom field";
	String DETECTED_AS_BOTTOM_FIELD_FIRST_USING_MULTIPLE_FRAME_DETECTION = "Detected as bottom field first, using multiple-frame detection";
	String DETECTED_AS_BOTTOM_FIELD_FIRST = "Detected as bottom field first";
	String WITH_THE_TOP_FIELD_REPEATED_FROM_THE_PREVIOUS_FRAME_S_TOP_FIELD = "With the top field repeated from the previous frame’s top field";
	String DETECTED_AS_TOP_FIELD_FIRST_USING_MULTIPLE_FRAME_DETECTION = "Detected as top field first, using multiple-frame detection";
	String DETECTED_AS_TOP_FIELD_FIRST = "Detected as top field first";
	String DETECTED_AS_PROGRESSIVE_USING_MULTIPLE_FRAME_DETECTION = "Detected as progressive, using multiple-frame detection";
	String DETECTED_AS_PROGRESSIVE = "Detected as progressive";

	String SNAPSHOT = "Snapshots images";
}
