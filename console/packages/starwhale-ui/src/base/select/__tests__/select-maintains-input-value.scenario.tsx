/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/
import * as React from 'react';
import { Select } from '..';

export function Scenario() {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [value, setValue] = React.useState<any>([]);
  return (
    <div>
      <div id="maintain-after-blur">
        maintain after blur
        <Select
          options={[
            { id: 'AliceBlue', color: '#F0F8FF' },
            { id: 'AntiqueWhite', color: '#FAEBD7' },
            { id: 'Aqua', color: '#00FFFF' },
            { id: 'Aquamarine', color: '#7FFFD4' },
            { id: 'Azure', color: '#F0FFFF' },
            { id: 'Beige', color: '#F5F5DC' },
          ]}
          labelKey="id"
          valueKey="color"
          onChange={({ value }) => setValue(value)}
          value={value}
          onBlurResetsInput={false}
          overrides={{ ClearIcon: { props: { 'data-id': 'clear-icon' } } }}
        />
      </div>

      <div id="maintain-after-close">
        maintain after close
        <Select
          options={[
            { id: 'AliceBlue', color: '#F0F8FF' },
            { id: 'AntiqueWhite', color: '#FAEBD7' },
            { id: 'Aqua', color: '#00FFFF' },
            { id: 'Aquamarine', color: '#7FFFD4' },
            { id: 'Azure', color: '#F0FFFF' },
            { id: 'Beige', color: '#F5F5DC' },
          ]}
          labelKey="id"
          valueKey="color"
          onChange={({ value }) => setValue(value)}
          value={value}
          onCloseResetsInput={false}
          overrides={{ ClearIcon: { props: { 'data-id': 'clear-icon' } } }}
        />
      </div>

      <div id="maintain-after-select">
        maintain after select
        <Select
          options={[
            { id: 'AliceBlue', color: '#F0F8FF' },
            { id: 'AntiqueWhite', color: '#FAEBD7' },
            { id: 'Aqua', color: '#00FFFF' },
            { id: 'Aquamarine', color: '#7FFFD4' },
            { id: 'Azure', color: '#F0FFFF' },
            { id: 'Beige', color: '#F5F5DC' },
          ]}
          labelKey="id"
          valueKey="color"
          onChange={({ value }) => setValue(value)}
          value={value}
          onSelectResetsInput={false}
          overrides={{ ClearIcon: { props: { 'data-id': 'clear-icon' } } }}
        />
      </div>

      <div id="maintain-after-all">
        maintain after all
        <Select
          options={[
            { id: 'AliceBlue', color: '#F0F8FF' },
            { id: 'AntiqueWhite', color: '#FAEBD7' },
            { id: 'Aqua', color: '#00FFFF' },
            { id: 'Aquamarine', color: '#7FFFD4' },
            { id: 'Azure', color: '#F0FFFF' },
            { id: 'Beige', color: '#F5F5DC' },
          ]}
          labelKey="id"
          valueKey="color"
          onChange={({ value }) => setValue(value)}
          value={value}
          onBlurResetsInput={false}
          onCloseResetsInput={false}
          onSelectResetsInput={false}
          overrides={{ ClearIcon: { props: { 'data-id': 'clear-icon' } } }}
        />
      </div>
    </div>
  );
}
