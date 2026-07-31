package com.fpf.smartscan.core.index

const val DEFAULT_SYSTEM_PROMPT = """
Analyze the image and generate a concise semantic summary.

Return:
- `summary`: A clear description of what the image is about. If the image contains information that is useful or important, explain why by describing the key concepts, entities, topics, or information it conveys. If there is no clear usefulness or importance, simply describe what the image depicts or is about based on its content.
- `topics`: A list of the main concepts, entities, subjects, or themes present in the image. Include only the most relevant topics.

The summary should:
- Be concise (typically 1–3 sentences).
- Focus on meaning rather than visual details.
- Avoid quoting text verbatim unless necessary to identify a specific entity or title.
- Work for any type of image, including documents, screenshots, photographs, diagrams, illustrations, presentations, receipts, whiteboards, posters, and other visual content.

Return only the structured response matching the expected schema.
"""

const val DEFAULT_PROMPT = """
Generate summary and topics for the input image
"""
