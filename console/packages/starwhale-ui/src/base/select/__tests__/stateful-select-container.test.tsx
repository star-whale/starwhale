/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/
import * as React from 'react';
import { act } from 'react-dom/test-utils';
import { render } from '@testing-library/react';

import { StatefulSelectContainer } from '..';
import { STATE_CHANGE_TYPE } from '../constants';

describe('StatefulSelectContainer', function () {
  let props: {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    children: jest.Mock<JSX.Element, [any]>;
    initialState: { value: { id: string; label: string }[] };
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    stateReducer: jest.Mock<any, any>;
    overrides: {};
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onChange: jest.Mock<any, any>;
  };

  beforeEach(function () {
    props = {
      // @ts-expect-error
      children: jest.fn(() => <div>test</div>),
      initialState: { value: [{ id: 'id', label: 'label' }] },
      stateReducer: jest.fn(),
      overrides: {},
      onChange: jest.fn(),
    };
  });

  it('provides props to children render func', function () {
    render(<StatefulSelectContainer {...props} />);
    const actualProps = props.children.mock.calls[0][0];
    expect(actualProps).toHaveProperty('value', props.initialState.value);
  });

  it('calls onChange handler with correct params', function () {
    render(<StatefulSelectContainer {...props} />);
    const newValue = { id: 'id2', label: 'label2' };
    const params = {
      value: [...props.initialState.value, newValue],
      option: newValue,
      type: STATE_CHANGE_TYPE.select,
    };
    const actualProps = props.children.mock.calls[0][0];
    act(() => {
      actualProps.onChange(params);
    });
    expect(props.onChange).toHaveBeenCalledWith(params);
  });
});
