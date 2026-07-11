package com.fpf.smartscan.constants

const val DEFAULT_SYSTEM_PROMPT = """
Determine whether the image is a text-based image, such as an article screenshot, Reddit post screenshot, document page, or book page. 
If the image is not primarily text-based, return `isTextBasedImage = false` and an empty `highlights` array.
If the image is text-based, extract the most important highlights from the text. 
Each highlight must be an exact quote copied from the image, preserving the original wording. 
Select quotes that capture key concepts, important information, notable entities, or meaningful statements.
"""

const val DEFAULT_PROMPT = """
Extract highlights from the input image
"""
