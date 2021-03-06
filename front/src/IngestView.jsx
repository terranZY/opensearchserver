/*
 * Copyright 2017-2020 Emmanuel Keller / Jaeksoft
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import {hot} from 'react-hot-loader/root';
import React, {useState, useEffect} from 'react';
import Status from "./Status";
import JsonEditor from "./JsonEditor";
import {fetchJson} from "./fetchUtils.js"

const IngestView = (props) => {

  const [error, setError] = useState('');
  const [task, setTask] = useState('');
  const [spinning, setSpinning] = useState(false);
  const [ingestResult, setIngestResult] = useState('');

  useEffect(() => {
    if (!props.indexJson) {
      doGenerateIndexSample();
    }
  }, [props.selectedIndex])

  return (
    <div className="tri-view">
      <div className="bg-light text-secondary p-1">INGEST&nbsp;
        <Status task={task} error={error} spinning={spinning}/>
      </div>
      <div className="central border bg-light">
        <div className="left-column border bg-light">
          <JsonEditor value={props.indexJson}
                      setValue={props.setIndexJson}/>
        </div>
        <div className="right-column border bg-light">
          <JsonEditor value={ingestResult}/>
        </div>
      </div>
      <form className="form-inline pr-1 pb-1">
        <div className="pt-1 pl-1">
          <button className="btn btn-outline-primary"
                  onClick={() => doGenerateIndexSample()}>
            Generate example
          </button>
        </div>
        <div className="pt-1 pl-1">
          <button className="btn btn-primary"
                  onClick={() => doIndex()}>
            INDEX
          </button>
        </div>
      </form>
    </div>
  )

  function checkIndex() {
    if (props.selectedIndex == null || props.selectedIndex === '') {
      setError('Please select an index.');
      return false;
    }
    return true;
  }

  function doGenerateIndexSample() {
    if (!checkIndex())
      return;
    setError(null);
    setIngestResult(null);
    setTask('Collecting sample...');
    setSpinning(true);
    fetchJson(
      props.oss + '/ws/indexes/' + props.selectedIndex + '/json/samples?count=1',
      {method: 'GET'},
      json => {
        props.setIndexJson(JSON.stringify(json, undefined, 2));
        setTask(null);
        setSpinning(false);
      },
      error => {
        setError(error);
        setTask(null);
        setSpinning(false);
      });
  }

  function parseJson() {
    const notParsed = props.indexJson;
    if (notParsed === null || notParsed === '') {
      throw 'Nothing to index';
    }
    return JSON.parse(notParsed);
  }

  function doIndex() {
    if (!checkIndex())
      return;
    setIngestResult(null);
    setError(null);
    setTask('Parsing...');
    setSpinning(true);
    var parsedJson = null;
    try {
      parsedJson = parseJson();
      props.setIndexJson(JSON.stringify(parsedJson, undefined, 2))
    } catch (err) {
      setError(err.message);
      setTask(null);
      setSpinning(false);
      return;
    }

    fetchJson(
      props.oss + '/ws/indexes/' + props.selectedIndex + '/json?fieldTypes=true',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(parsedJson)
      },
      json => {
        setIngestResult(JSON.stringify(json, undefined, 2));
        var msg;
        switch (json.count) {
          case 0:
            msg = 'Nothing has been indexed.';
            break;
          case 1:
            msg = 'One record has been indexed.';
            break;
          default:
            msg = json.count + ' records have been indexed.';
            break;
        }
        setTask(msg);
        setSpinning(false);
      },
      error => {
        setError(error);
        setTask(null);
        setSpinning(false);
      });
  }
}

export default hot(IngestView);
