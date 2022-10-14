import os

def rstjinja(app, docname, source):
		"""
		Render our pages as a jinja template for fancy templating goodness.
		"""
		# Make sure we're outputting HTML
		if app.builder.format != 'html':
				return
		src = source[0]

		additional_context = {
		 'current_version': os.environ.get('SPHINX_MULTIVERSION_NAME'),
		}

		rendered = app.builder.templates.render_string(
				src, {**app.config.html_context,  **additional_context}
		)
		source[0] = rendered

def setup(app):
		app.setup_extension('sphinx_multiversion')
		app.connect("source-read", rstjinja)

