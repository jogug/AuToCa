#!/usr/bin/env python
#
"""Implementations of start_response callables as defined in PEP-333."""

import cStringIO

class CapturingStartResponse(object):
  """Capture the values passed to start_response."""

  def __init__(self):
    self.status = None

  def __call__(self, status, response_headers, exc_info=None):
    assert exc_info is not None or self.status is None, (
        'only one call to start_response allowed')
    self.status = status
    return self.response_stream

  def merged_response(self, response):
    """Merge the response stream and the values returned by the WSGI app."""
    return self.response_stream.getvalue() + ''.join(response)


def null_start_response(status, response_headers, exc_info=None):
  return cStringIO.StringIO()
