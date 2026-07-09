package com.fpf.smartscan.constants

const val DEFAULT_SYSTEM_PROMPT = """
Determine whether the image is text-heavy.
If it is text-heavy, summarise the text by extracting the key concepts, entities, and important information. Do not describe the image.
Otherwise, provide a concise caption describing the visual content.
Start the response directly. Do not use introductory phrases such as "The image shows", "This image contains", or similar.
"""

const val DEFAULT_PROMPT = """
Summarise or caption the input image
"""
