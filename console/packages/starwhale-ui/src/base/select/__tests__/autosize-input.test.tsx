/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/
import * as React from 'react';
import { render } from '@testing-library/react';

import AutosizeInput from '../autosize-input';

describe('AutosizeInput component', function () {
  it('renders correctly', function () {
    const { container } = render(
      // @ts-expect-error
      <AutosizeInput value="test" onChange={jest.fn()} />
    );
    const input = container.querySelector('input');
    expect(input).not.toBeNull();
    expect(input?.getAttribute('value')).toBe('test');
  });
});
