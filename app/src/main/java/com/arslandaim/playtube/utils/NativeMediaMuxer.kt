/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

class NativeMediaMuxer {

    @Throws(Exception::class)
    fun mux(videoFile: File, audioFile: File, outputFile: File) {
        PTLog.d("NativeMediaMuxer", "Starting interleaved muxing: video=${videoFile.name}, audio=${audioFile.name}")
        
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        
        var videoFis: FileInputStream? = null
        var audioFis: FileInputStream? = null

        try {
            videoFis = FileInputStream(videoFile)
            audioFis = FileInputStream(audioFile)
            videoExtractor.setDataSource(videoFis.fd)
            audioExtractor.setDataSource(audioFis.fd)

            val outputFormat = if (outputFile.extension.equals("webm", true)) {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
            } else {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            }

            muxer = MediaMuxer(outputFile.absolutePath, outputFormat)

            var videoTrackIndex = -1
            var audioTrackIndex = -1

            // Setup Video Track
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                    videoExtractor.selectTrack(i)
                    videoTrackIndex = muxer.addTrack(format)
                    break
                }
            }

            // Setup Audio Track
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioExtractor.selectTrack(i)
                    audioTrackIndex = muxer.addTrack(format)
                    break
                }
            }

            if (videoTrackIndex == -1 || audioTrackIndex == -1) {
                throw Exception("Required tracks missing: video=$videoTrackIndex, audio=$audioTrackIndex")
            }

            muxer.start()

            // Seek to start and calculate offsets for normalization
            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            
            val videoStartOffset = videoExtractor.sampleTime
            val audioStartOffset = audioExtractor.sampleTime

            val videoBuffer = ByteBuffer.allocateDirect(4 * 1024 * 1024)
            val audioBuffer = ByteBuffer.allocateDirect(1 * 1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            var videoDone = false
            var audioDone = false

            // Unified loop to interleave video and audio samples chronologically
            while (!videoDone || !audioDone) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException("Muxing interrupted")

                val videoTime = if (!videoDone) videoExtractor.sampleTime else Long.MAX_VALUE
                val audioTime = if (!audioDone) audioExtractor.sampleTime else Long.MAX_VALUE

                if (!videoDone && videoTime <= audioTime) {
                    bufferInfo.size = videoExtractor.readSampleData(videoBuffer, 0)
                    if (bufferInfo.size < 0) {
                        videoDone = true
                    } else {
                        bufferInfo.presentationTimeUs = videoExtractor.sampleTime - videoStartOffset
                        bufferInfo.offset = 0
                        bufferInfo.flags = if ((videoExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                            MediaCodec.BUFFER_FLAG_KEY_FRAME
                        } else 0
                        
                        muxer.writeSampleData(videoTrackIndex, videoBuffer, bufferInfo)
                        videoExtractor.advance()
                    }
                } else if (!audioDone) {
                    bufferInfo.size = audioExtractor.readSampleData(audioBuffer, 0)
                    if (bufferInfo.size < 0) {
                        audioDone = true
                    } else {
                        bufferInfo.presentationTimeUs = audioExtractor.sampleTime - audioStartOffset
                        bufferInfo.offset = 0
                        bufferInfo.flags = if ((audioExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                            MediaCodec.BUFFER_FLAG_KEY_FRAME
                        } else 0
                        
                        muxer.writeSampleData(audioTrackIndex, audioBuffer, bufferInfo)
                        audioExtractor.advance()
                    }
                }
            }

            PTLog.d("NativeMediaMuxer", "Muxing successful: ${outputFile.length()} bytes")
        } catch (e: Exception) {
            PTLog.e("NativeMediaMuxer", "Muxing failed", e)
            throw e
        } finally {
            try { muxer?.stop() } catch (e: Exception) {}
            try { muxer?.release() } catch (e: Exception) {}
            try { videoExtractor.release() } catch (e: Exception) {}
            try { audioExtractor.release() } catch (e: Exception) {}
            try { videoFis?.close() } catch (e: Exception) {}
            try { audioFis?.close() } catch (e: Exception) {}
        }
    }
}
